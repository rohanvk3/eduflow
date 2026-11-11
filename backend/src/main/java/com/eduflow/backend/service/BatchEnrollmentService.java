package com.eduflow.backend.service;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import java.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true) @PreAuthorize("hasRole('ADMIN')")
public class BatchEnrollmentService {
    private final BatchRepository batches; private final StudentRepository students; private final BatchEnrollmentRepository enrollments; private final AcademicAccess access;
    public BatchEnrollmentService(BatchRepository batches,StudentRepository students,BatchEnrollmentRepository enrollments,AcademicAccess access) { this.batches=batches; this.students=students; this.enrollments=enrollments; this.access=access; }
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public PageResponse<EnrollmentResponse> list(Long batchId,int page,int size) {
        access.readRoster(batchId); CourseService.validatePage(page,size,"");
        if(!batches.existsById(batchId)) throw new ResourceNotFoundException("Batch not found");
        return PageResponse.from(enrollments.findAll((root,q,cb)->cb.equal(root.get("batch").get("id"),batchId),PageRequest.of(page,size,Sort.by(Sort.Order.desc("enrollmentDate"),Sort.Order.desc("id")))).map(this::response));
    }
    @PreAuthorize("hasRole('STUDENT')")
    public PageResponse<EnrollmentResponse> mine(int page,int size) {
        CourseService.validatePage(page,size,"");
        Specification<BatchEnrollment> spec=(root,q,cb)->cb.equal(root.get("student").get("user").get("email"),access.email());
        return PageResponse.from(enrollments.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Order.desc("enrollmentDate"),Sort.Order.desc("id")))).map(this::response));
    }
    @Transactional public EnrollmentResponse enroll(Long batchId,EnrollmentRequest r) {
        Batch b=lock(batchId); access.editable(b);
        Student student=students.findById(r.studentId()).orElseThrow(()->new ResourceNotFoundException("Student not found"));
        if(student.getStatus()!=StudentStatus.ACTIVE) throw new AcademicConflictException("Only active students can enroll");
        LocalDate date=r.enrollmentDate();
        if(date.isAfter(BatchService.today()) || date.isAfter(b.getEndDate()) || date.isBefore(student.getAdmissionDate())) throw new IllegalArgumentException("Enrollment date must be on or after admission, not in the future, and not after batch end");
        for(var old:enrollments.findByBatchIdAndStudentIdOrderByEnrollmentDate(batchId,r.studentId())) {
            if(old.getStatus()==EnrollmentStatus.ACTIVE) throw new AcademicConflictException("Student is already enrolled");
            if(old.getWithdrawalDate()==null || !date.isAfter(old.getWithdrawalDate())) throw new AcademicConflictException("Re-enrollment must be after the prior withdrawal date");
        }
        if(enrollments.countByBatchIdAndStatus(batchId,EnrollmentStatus.ACTIVE)>=b.getCapacity()) throw new AcademicConflictException("Batch capacity is full");
        BatchEnrollment e=new BatchEnrollment(); e.setBatch(b); e.setStudent(student); e.setEnrollmentDate(date);
        try { return response(enrollments.saveAndFlush(e)); } catch(DataIntegrityViolationException ex) { throw new AcademicConflictException("Enrollment period already exists"); }
    }
    @Transactional public EnrollmentResponse withdraw(Long batchId,Long enrollmentId,WithdrawalRequest r) {
        lock(batchId);
        BatchEnrollment e=enrollments.findById(enrollmentId).filter(v->v.getBatch().getId().equals(batchId)).orElseThrow(()->new ResourceNotFoundException("Enrollment not found"));
        if(e.getStatus()!=EnrollmentStatus.ACTIVE) throw new AcademicConflictException("Only active enrollment can be withdrawn");
        if(!r.withdrawalDate().equals(BatchService.today()) || r.withdrawalDate().isBefore(e.getEnrollmentDate())) throw new IllegalArgumentException("Withdrawal must be effective today and not before enrollment");
        e.setWithdrawalDate(r.withdrawalDate()); e.setStatus(EnrollmentStatus.WITHDRAWN);
        return response(enrollments.saveAndFlush(e));
    }
    private Batch lock(Long id) { return batches.findLockedById(id).orElseThrow(()->new ResourceNotFoundException("Batch not found")); }
    private EnrollmentResponse response(BatchEnrollment e) { return new EnrollmentResponse(e.getId(),e.getBatch().getId(),e.getBatch().getName(),e.getStudent().getId(),e.getStudent().getFullName(),e.getStudent().getAdmissionNumber(),e.getEnrollmentDate(),e.getWithdrawalDate(),e.getCompletionDate(),e.getStatus()); }
}
