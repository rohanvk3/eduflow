package com.eduflow.backend.service;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class BatchService {
    private final BatchRepository batches;
    private final CourseRepository courses;
    private final BatchEnrollmentRepository enrollments;
    private final BatchScheduleRepository schedules;
    private final AcademicAccess access;
    public BatchService(BatchRepository batches,CourseRepository courses,BatchEnrollmentRepository enrollments,BatchScheduleRepository schedules,AcademicAccess access) {
        this.batches=batches; this.courses=courses; this.enrollments=enrollments; this.schedules=schedules; this.access=access;
    }
    public PageResponse<BatchResponse> list(int page,int size,String search,Long courseId,BatchStatus status) {
        CourseService.validatePage(page,size,search); String pattern=CourseService.pattern(search);
        Specification<Batch> spec=(root,query,cb)-> {
            var match=cb.or(cb.like(cb.lower(root.get("name")),pattern,'\\'),cb.like(cb.lower(root.get("code")),pattern,'\\'));
            if(courseId!=null) match=cb.and(match,cb.equal(root.get("course").get("id"),courseId));
            if(status!=null) match=cb.and(match,cb.equal(root.get("status"),status));
            if(!access.hasRole("ADMIN")) {
                var sub=query.subquery(Long.class);
                if(access.hasRole("TEACHER")) {
                    var a=sub.from(BatchTeacherAssignment.class);
                    sub.select(a.get("batch").get("id")).where(cb.equal(a.get("teacher").get("user").get("email"),access.email()),cb.isTrue(a.get("active")));
                } else {
                    var e=sub.from(BatchEnrollment.class);
                    sub.select(e.get("batch").get("id")).where(cb.equal(e.get("student").get("user").get("email"),access.email()));
                }
                match=cb.and(match,root.get("id").in(sub));
            }
            return match;
        };
        return PageResponse.from(batches.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Order.desc("startDate"),Sort.Order.desc("id")))).map(this::response));
    }
    public BatchResponse get(Long id) { access.readBatch(id); return response(find(id)); }
    @PreAuthorize("hasRole('ADMIN')") public Map<String,Long> stats() { return Map.of("total",batches.count(),"active",batches.countByStatus(BatchStatus.ACTIVE)); }
    @Transactional @PreAuthorize("hasRole('ADMIN')") public BatchResponse create(BatchRequest r) { return save(new Batch(),r); }
    @Transactional @PreAuthorize("hasRole('ADMIN')") public BatchResponse update(Long id,BatchRequest r) {
        Batch b=batches.findLockedById(id).orElseThrow(()->new ResourceNotFoundException("Batch not found")); access.editable(b);
        return save(b,r);
    }
    private BatchResponse save(Batch b,BatchRequest r) {
        if(r.endDate().isBefore(r.startDate())) throw new IllegalArgumentException("End date must not precede start date");
        if(r.status()==BatchStatus.COMPLETED && today().isBefore(r.endDate())) throw new IllegalArgumentException("Batch cannot complete before its end date");
        boolean linked=b.getId()!=null && (enrollments.existsByBatchId(b.getId()) || schedules.existsByBatchId(b.getId()));
        if(linked && (!b.getCourse().getId().equals(r.courseId()) || !b.getStartDate().equals(r.startDate()) || !b.getEndDate().equals(r.endDate())))
            throw new AcademicConflictException("Course and date range are fixed once enrollment or timetable history exists");
        long occupied=b.getId()==null?0:enrollments.countByBatchIdAndStatus(b.getId(),EnrollmentStatus.ACTIVE);
        if(r.capacity()<occupied) throw new AcademicConflictException("Capacity cannot be lower than active enrollment count");
        Course course=courses.findById(r.courseId()).orElseThrow(()->new ResourceNotFoundException("Course not found"));
        if((b.getId()==null || !b.getCourse().getId().equals(course.getId())) && course.getStatus()!=StudentStatus.ACTIVE) throw new AcademicConflictException("An active course is required");
        String code=r.code().toUpperCase(Locale.ROOT);
        if(batches.existsByCodeIgnoreCaseAndIdNot(code,b.getId()==null?0L:b.getId())) throw new AcademicConflictException("Batch code already exists");
        b.setCode(code); b.setName(r.name().strip()); b.setCourse(course); b.setCapacity(r.capacity()); b.setStartDate(r.startDate()); b.setEndDate(r.endDate()); b.setStatus(r.status());
        try { batches.saveAndFlush(b); } catch(DataIntegrityViolationException e) { throw new AcademicConflictException("Batch code already exists"); }
        if(b.getStatus()==BatchStatus.COMPLETED) {
            for(var e:enrollments.findByBatchIdAndStatus(b.getId(),EnrollmentStatus.ACTIVE)) { e.setStatus(EnrollmentStatus.COMPLETED); e.setCompletionDate(b.getEndDate()); }
            enrollments.flush();
        }
        return response(b);
    }
    private Batch find(Long id) { return batches.findById(id).orElseThrow(()->new ResourceNotFoundException("Batch not found")); }
    private BatchResponse response(Batch b) { return new BatchResponse(b.getId(),b.getCode(),b.getName(),b.getCourse().getId(),b.getCourse().getName(),b.getCapacity(),enrollments.countByBatchIdAndStatus(b.getId(),EnrollmentStatus.ACTIVE),b.getStartDate(),b.getEndDate(),b.getStatus()); }
    public static LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Kolkata")); }
}
