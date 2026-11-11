package com.eduflow.backend.service;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.*;
import jakarta.persistence.criteria.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AttendanceScope {
    private final AcademicAccess access;
    private final BatchEnrollmentRepository enrollments;
    private final BatchTeacherAssignmentRepository assignments;
    public AttendanceScope(AcademicAccess access,BatchEnrollmentRepository enrollments,BatchTeacherAssignmentRepository assignments) { this.access=access;this.enrollments=enrollments;this.assignments=assignments; }
    public void staffBatch(Long batchId) { access.readRoster(batchId); }
    public boolean admin() { return access.hasRole("ADMIN"); }
    public boolean teacher() { return access.hasRole("TEACHER"); }
    public String email() { return access.email(); }
    public Predicate sessionPredicate(Root<AttendanceSession> root,CriteriaQuery<?> query,CriteriaBuilder cb) {
        if(admin()) return cb.conjunction();
        var sub=query.subquery(Long.class);var a=sub.from(BatchTeacherAssignment.class);
        sub.select(a.get("batch").get("id")).where(cb.isTrue(a.get("active")),cb.equal(a.get("teacher").get("user").get("email"),email()));
        return root.get("batch").get("id").in(sub);
    }
    public void student(Long id,Student own) {
        if(admin()) return;
        if(access.hasRole("STUDENT")) { if(own!=null && own.getId().equals(id)) return; throw new AccessDeniedException("You can only view your own attendance"); }
        for(var e:enrollments.findAll((root,q,cb)->cb.equal(root.get("student").get("id"),id)))
            if(assignments.existsByBatchIdAndTeacherUserEmailAndActiveTrue(e.getBatch().getId(),email())) return;
        throw new AccessDeniedException("Student attendance access denied");
    }
}
