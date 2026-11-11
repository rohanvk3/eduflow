package com.eduflow.backend.service;

import com.eduflow.backend.dto.*;
import com.eduflow.backend.dto.AttendanceDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class AttendanceSessionService {
    private final AttendanceSessionRepository sessions;
    private final AttendanceRecordRepository records;
    private final BatchRepository batches;
    private final BatchScheduleRepository schedules;
    private final BatchEnrollmentRepository enrollments;
    private final UserRepository users;
    private final AttendanceScope scope;
    private final NotificationOutbox notifications;
    public AttendanceSessionService(AttendanceSessionRepository sessions,AttendanceRecordRepository records,BatchRepository batches,BatchScheduleRepository schedules,BatchEnrollmentRepository enrollments,UserRepository users,AttendanceScope scope,NotificationOutbox notifications) {
        this.sessions=sessions;this.records=records;this.batches=batches;this.schedules=schedules;this.enrollments=enrollments;this.users=users;this.scope=scope;this.notifications=notifications;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public SessionDetails create(CreateSession r) {
        Batch batch=lockBatch(r.batchId());scope.staffBatch(batch.getId());
        if(batch.getStatus()==BatchStatus.CANCELLED || batch.getStatus()==BatchStatus.COMPLETED) throw new AttendanceConflictException("Cannot create attendance for a cancelled or completed batch");
        if(r.sessionDate().isBefore(batch.getStartDate()) || r.sessionDate().isAfter(batch.getEndDate())) throw new IllegalArgumentException("Session date must fall within batch dates");
        AttendanceSession s=new AttendanceSession();s.setBatch(batch);s.setSessionDate(r.sessionDate());s.setTopic(r.topic());
        LocalTime start=r.startTime(),end=r.endTime();
        if(r.scheduleId()!=null) {
            var schedule=schedules.findById(r.scheduleId()).filter(v->v.getBatch().getId().equals(batch.getId())).orElseThrow(()->new ResourceNotFoundException("Batch schedule not found"));
            if(!schedule.getActive()) throw new AttendanceConflictException("Schedule is inactive");
            if(schedule.getDayOfWeek()!=r.sessionDate().getDayOfWeek()) throw new IllegalArgumentException("Session date does not match the weekly schedule");
            if((start!=null && !start.equals(schedule.getStartTime())) || (end!=null && !end.equals(schedule.getEndTime()))) throw new IllegalArgumentException("Scheduled session times must match its weekly class");
            s.setSchedule(schedule);start=schedule.getStartTime();end=schedule.getEndTime();
        }
        if((start==null)!=(end==null)) throw new IllegalArgumentException("Provide both start and end time or neither");
        if(start!=null && (!end.isAfter(start) || start.getSecond()!=0 || end.getSecond()!=0 || start.getNano()!=0 || end.getNano()!=0)) throw new IllegalArgumentException("Use minute precision and end time after start time");
        s.setStartTime(start);s.setEndTime(end);s.setOccurrenceKey(start==null?"UNTIMED":start.format(DateTimeFormatter.ofPattern("HH:mm")));
        if(sessions.existsByBatchIdAndSessionDateAndOccurrenceKey(batch.getId(),r.sessionDate(),s.getOccurrenceKey())) throw new AttendanceConflictException("A session already exists for this batch, date and start time");
        s.setMarkedBy(currentUser());
        try { sessions.saveAndFlush(s); } catch(DataIntegrityViolationException e) { throw new AttendanceConflictException("This class occurrence already has an attendance session"); }
        return details(s);
    }
    public SessionDetails get(Long id) { var s=find(id);scope.staffBatch(s.getBatch().getId());return details(s); }
    public PageResponse<SessionSummary> list(Long batchId,LocalDate from,LocalDate to,AttendanceSessionStatus status,int page,int size,String sort) {
        var range=AttendanceReportService.range(from,to);CourseService.validatePage(page,size,"");if(page>100000)throw new IllegalArgumentException("Page too large");
        if(batchId!=null) { scope.staffBatch(batchId);if(!batches.existsById(batchId))throw new ResourceNotFoundException("Batch not found"); }
        String[] ordering=sort.split(",",-1);
        if(ordering.length!=2 || !Set.of("sessionDate","startTime","createdAt","status").contains(ordering[0]) || !Set.of("asc","desc").contains(ordering[1]))throw new IllegalArgumentException("Invalid attendance sort");
        var result=sessions.findAll(spec(batchId,range[0],range[1],status),PageRequest.of(page,size,Sort.by(Sort.Direction.fromString(ordering[1]),ordering[0]).and(Sort.by(Sort.Order.desc("id")))));
        return PageResponse.from(result.map(this::summary));
    }
    public long pending(Long batchId,LocalDate from,LocalDate to) { return sessions.count(spec(batchId,from,to,AttendanceSessionStatus.DRAFT)); }
    private Specification<AttendanceSession> spec(Long batchId,LocalDate from,LocalDate to,AttendanceSessionStatus status) {
        return(root,q,cb)->{
            var p=cb.and(scope.sessionPredicate(root,q,cb),cb.between(root.get("sessionDate"),from,to));
            if(batchId!=null)p=cb.and(p,cb.equal(root.get("batch").get("id"),batchId));
            if(status!=null)p=cb.and(p,cb.equal(root.get("status"),status));return p;
        };
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public SessionDetails save(Long id,SaveRecords request) {
        var s=lockSession(id);draft(s,request.version());
        Map<Long,Student> eligible=eligible(s);Set<Long> seen=new HashSet<>();
        // Validate the entire replacement before changing any persisted record.
        for(var r:request.records()) {
            if(!seen.add(r.studentId()))throw new IllegalArgumentException("Duplicate student entries are not allowed");
            if(!eligible.containsKey(r.studentId()))throw new IllegalArgumentException("Student "+r.studentId()+" is not eligible for this class date");
        }
        records.deleteForSession(id);
        List<AttendanceRecord> replacement=new ArrayList<>();
        for(var r:request.records()) { var record=new AttendanceRecord();record.setSession(s);record.setStudent(eligible.get(r.studentId()));record.setStatus(r.status());record.setRemarks(r.remarks());replacement.add(record); }
        records.saveAllAndFlush(replacement);s.setMarkedBy(currentUser());s.touch();sessions.saveAndFlush(s);return details(s);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public SessionDetails finalizeSession(Long id,FinalizeRequest request) {
        var s=lockSession(id);draft(s,request==null?null:request.version());
        if(s.getSessionDate().isAfter(BatchService.today()))throw new IllegalArgumentException("Future attendance cannot be finalized");
        Set<Long> expected=eligible(s).keySet();
        Set<Long> marked=new HashSet<>();for(var r:records.findBySessionIdOrderByStudentFullNameAscStudentIdAsc(id))marked.add(r.getStudent().getId());
        if(!marked.equals(expected))throw new AttendanceConflictException("Every eligible student must be marked. Reload and save the full current register before finalizing");
        s.setStatus(AttendanceSessionStatus.FINALIZED);s.setFinalizedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));s.setMarkedBy(currentUser());sessions.saveAndFlush(s);notifications.capture("attendance-finalized:"+id,NotificationType.ATTENDANCE_FINALIZED,"ATTENDANCE",id,null);return details(s);
    }
    private AttendanceSession lockSession(Long id) {
        Long batchId=sessions.findBatchIdById(id).orElseThrow(()->new ResourceNotFoundException("Attendance session not found"));
        lockBatch(batchId);scope.staffBatch(batchId);
        return sessions.findLockedById(id).orElseThrow(()->new ResourceNotFoundException("Attendance session not found"));
    }
    private void draft(AttendanceSession s,Long version) {
        if(s.getStatus()!=AttendanceSessionStatus.DRAFT)throw new AttendanceConflictException("Finalized attendance is read-only; ordinary edits are not allowed");
        if(version!=null && !Objects.equals(version,s.getVersion()))throw new AttendanceConflictException("Attendance changed; reload before saving");
    }
    private Map<Long,Student> eligible(AttendanceSession s) {
        Map<Long,Student> result=new LinkedHashMap<>();
        for(var e:enrollments.findEligibleOn(s.getBatch().getId(),s.getSessionDate()))result.put(e.getStudent().getId(),e.getStudent());
        return result;
    }
    private SessionDetails details(AttendanceSession s) {
        List<AttendanceRecord> saved=records.findBySessionIdOrderByStudentFullNameAscStudentIdAsc(s.getId());
        List<RegisterRow> roster=new ArrayList<>();long stale=0;
        if(s.getStatus()==AttendanceSessionStatus.FINALIZED) {
            // A finalized record proves eligibility at finalization; later withdrawals do not erase it.
            for(var r:saved)roster.add(row(r.getStudent(),r));
        } else {
            Map<Long,AttendanceRecord> map=new HashMap<>();for(var r:saved)map.put(r.getStudent().getId(),r);
            var eligible=eligible(s);for(var student:eligible.values())roster.add(row(student,map.get(student.getId())));
            stale=saved.stream().filter(r->!eligible.containsKey(r.getStudent().getId())).count();
        }
        roster.sort(Comparator.comparing(RegisterRow::fullName).thenComparing(RegisterRow::studentId));
        return new SessionDetails(summary(s),roster,stale);
    }
    private RegisterRow row(Student student,AttendanceRecord r) { return new RegisterRow(student.getId(),student.getFullName(),student.getAdmissionNumber(),r==null?null:r.getStatus(),r==null?null:r.getRemarks()); }
    private SessionSummary summary(AttendanceSession s) {
        long marked=records.countBySessionId(s.getId());long eligible=s.getStatus()==AttendanceSessionStatus.FINALIZED?marked:enrollments.countEligibleOn(s.getBatch().getId(),s.getSessionDate());
        return new SessionSummary(s.getId(),s.getBatch().getId(),s.getBatch().getName(),s.getBatch().getCourse().getId(),s.getBatch().getCourse().getName(),s.getSessionDate(),s.getSchedule()==null?null:s.getSchedule().getId(),s.getStartTime(),s.getEndTime(),s.getTopic(),s.getStatus(),s.getMarkedBy().getFullName(),s.getFinalizedAt(),s.getVersion(),eligible,marked);
    }
    private Batch lockBatch(Long id) { return batches.findLockedById(id).orElseThrow(()->new ResourceNotFoundException("Batch not found")); }
    private AttendanceSession find(Long id) { return sessions.findById(id).orElseThrow(()->new ResourceNotFoundException("Attendance session not found")); }
    private User currentUser() { return users.findByEmail(scope.email()).orElseThrow(()->new ResourceNotFoundException("User not found")); }
}
