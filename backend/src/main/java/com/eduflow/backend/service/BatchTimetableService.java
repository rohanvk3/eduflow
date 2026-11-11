package com.eduflow.backend.service;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class BatchTimetableService {
    private final BatchRepository batches; private final TeacherRepository teachers;
    private final BatchTeacherAssignmentRepository assignments; private final BatchScheduleRepository schedules;
    private final AcademicAccess access;
    public BatchTimetableService(BatchRepository batches,TeacherRepository teachers,BatchTeacherAssignmentRepository assignments,BatchScheduleRepository schedules,AcademicAccess access) {
        this.batches=batches; this.teachers=teachers; this.assignments=assignments; this.schedules=schedules; this.access=access;
    }
    public List<TeacherAssignmentResponse> teachers(Long batchId) {
        access.readBatch(batchId); exists(batchId);
        return assignments.findByBatchIdOrderById(batchId).stream().map(this::assignmentResponse).toList();
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public TeacherAssignmentResponse assign(Long batchId,TeacherAssignmentRequest r) {
        Batch b=lockBatch(batchId); access.editable(b); Teacher teacher=lockTeacher(r.teacherId());
        if(teacher.getStatus()!=StudentStatus.ACTIVE) throw new AcademicConflictException("Only active teachers can be assigned");
        var existing=assignments.findByBatchIdAndTeacherId(batchId,r.teacherId());
        if(existing.isPresent() && existing.get().getActive()) throw new AcademicConflictException("Teacher is already assigned");
        var a=existing.orElseGet(BatchTeacherAssignment::new); a.setBatch(b); a.setTeacher(teacher); a.setActive(true);
        try { return assignmentResponse(assignments.saveAndFlush(a)); } catch(DataIntegrityViolationException e) { throw new AcademicConflictException("Assignment already exists"); }
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public TeacherAssignmentResponse unassign(Long batchId,Long teacherId) {
        Batch b=lockBatch(batchId); access.editable(b); lockTeacher(teacherId);
        var a=assignments.findByBatchIdAndTeacherId(batchId,teacherId).orElseThrow(()->new ResourceNotFoundException("Assignment not found"));
        a.setActive(false);
        for(var s:schedules.findByBatchIdAndActiveTrue(batchId)) if(s.getTeacher().getId().equals(teacherId)) s.setActive(false);
        schedules.flush(); return assignmentResponse(assignments.saveAndFlush(a));
    }
    public List<ScheduleResponse> schedules(Long batchId) {
        access.readBatch(batchId); exists(batchId);
        return schedules.findByBatchIdOrderByDayOfWeekAscStartTimeAsc(batchId).stream().filter(s->access.hasRole("ADMIN") || s.getActive()).sorted(Comparator.comparing((BatchSchedule s)->s.getDayOfWeek().getValue()).thenComparing(BatchSchedule::getStartTime)).map(this::response).toList();
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public ScheduleResponse create(Long batchId,ScheduleRequest r) { return save(batchId,new BatchSchedule(),r); }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public ScheduleResponse update(Long batchId,Long scheduleId,ScheduleRequest r) {
        // Lock the batch before loading its schedule, consistently with create/unassign.
        Batch b=lockBatch(batchId); access.editable(b);
        var s=schedules.findById(scheduleId).filter(v->v.getBatch().getId().equals(batchId)).orElseThrow(()->new ResourceNotFoundException("Schedule not found"));
        return save(batchId,s,r);
    }
    private ScheduleResponse save(Long batchId,BatchSchedule s,ScheduleRequest r) {
        Batch b=lockBatch(batchId); access.editable(b); Teacher teacher=lockTeacher(r.teacherId());
        if(!r.endTime().isAfter(r.startTime()) || r.startTime().getSecond()!=0 || r.endTime().getSecond()!=0 || r.startTime().getNano()!=0 || r.endTime().getNano()!=0)
            throw new IllegalArgumentException("Use minute precision and an end time after start time; overnight classes are not supported");
        if(r.active()) {
            if(teacher.getStatus()!=StudentStatus.ACTIVE || !assignments.existsByBatchIdAndTeacherIdAndActiveTrue(batchId,r.teacherId())) throw new AcademicConflictException("An active assigned teacher is required");
            if(!occurs(b.getStartDate(),b.getEndDate(),r.dayOfWeek())) throw new IllegalArgumentException("This weekday does not occur within the batch date range");
            for(var old:schedules.findByTeacherIdAndActiveTrue(r.teacherId())) {
                if(Objects.equals(old.getId(),s.getId()) || old.getBatch().getStatus()==BatchStatus.CANCELLED || old.getBatch().getStatus()==BatchStatus.COMPLETED) continue;
                LocalDate start=b.getStartDate().isAfter(old.getBatch().getStartDate())?b.getStartDate():old.getBatch().getStartDate();
                LocalDate end=b.getEndDate().isBefore(old.getBatch().getEndDate())?b.getEndDate():old.getBatch().getEndDate();
                if(old.getDayOfWeek()==r.dayOfWeek() && occurs(start,end,r.dayOfWeek()) && overlaps(old,r)) throw new AcademicConflictException("Teacher has an overlapping class in this date range");
            }
            for(var old:schedules.findByBatchIdAndActiveTrue(batchId))
                if(!Objects.equals(old.getId(),s.getId()) && old.getDayOfWeek()==r.dayOfWeek() && overlaps(old,r)) throw new AcademicConflictException("Batch already has an overlapping class");
        }
        s.setBatch(b); s.setTeacher(teacher); s.setDayOfWeek(r.dayOfWeek()); s.setStartTime(r.startTime()); s.setEndTime(r.endTime()); s.setRoom(r.room()); s.setActive(r.active());
        return response(schedules.saveAndFlush(s));
    }
    private boolean overlaps(BatchSchedule old,ScheduleRequest r) { return r.startTime().isBefore(old.getEndTime()) && r.endTime().isAfter(old.getStartTime()); }
    private boolean occurs(LocalDate start,LocalDate end,DayOfWeek day) { return !start.isAfter(end) && !start.plusDays((day.getValue()-start.getDayOfWeek().getValue()+7)%7).isAfter(end); }
    private Batch lockBatch(Long id) { return batches.findLockedById(id).orElseThrow(()->new ResourceNotFoundException("Batch not found")); }
    private Teacher lockTeacher(Long id) { return teachers.findLockedById(id).orElseThrow(()->new ResourceNotFoundException("Teacher not found")); }
    private void exists(Long id) { if(!batches.existsById(id)) throw new ResourceNotFoundException("Batch not found"); }
    private TeacherAssignmentResponse assignmentResponse(BatchTeacherAssignment a) { return new TeacherAssignmentResponse(a.getId(),a.getTeacher().getId(),a.getTeacher().getFullName(),a.getTeacher().getEmployeeId(),a.getActive()); }
    private ScheduleResponse response(BatchSchedule s) { return new ScheduleResponse(s.getId(),s.getBatch().getId(),s.getTeacher().getId(),s.getTeacher().getFullName(),s.getDayOfWeek(),s.getStartTime(),s.getEndTime(),s.getRoom(),s.getActive()); }
}
