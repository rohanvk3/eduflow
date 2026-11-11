package com.eduflow.backend.service;

import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AcademicAccess {
    private final BatchTeacherAssignmentRepository assignments;
    private final BatchEnrollmentRepository enrollments;
    public AcademicAccess(BatchTeacherAssignmentRepository assignments, BatchEnrollmentRepository enrollments) {
        this.assignments=assignments; this.enrollments=enrollments;
    }
    public String email() { return SecurityContextHolder.getContext().getAuthentication().getName(); }
    public boolean hasRole(String role) { return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+role)); }
    public void readBatch(Long id) {
        if (hasRole("ADMIN")) return;
        if (hasRole("TEACHER") && assignments.existsByBatchIdAndTeacherUserEmailAndActiveTrue(id,email())) return;
        if (hasRole("STUDENT") && enrollments.existsByBatchIdAndStudentUserEmail(id,email())) return;
        throw new AccessDeniedException("Batch access denied");
    }
    public void readRoster(Long id) {
        if (hasRole("ADMIN") || (hasRole("TEACHER") && assignments.existsByBatchIdAndTeacherUserEmailAndActiveTrue(id,email()))) return;
        throw new AccessDeniedException("Roster access denied");
    }
    public void editable(Batch batch) {
        if (batch.getStatus()==BatchStatus.COMPLETED || batch.getStatus()==BatchStatus.CANCELLED)
            throw new AcademicConflictException("Completed or cancelled batches cannot be modified");
    }
}
