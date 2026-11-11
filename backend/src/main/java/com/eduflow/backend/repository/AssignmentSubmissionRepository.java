package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission,Long>,JpaSpecificationExecutor<AssignmentSubmission>{
 Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId,Long studentId);
 long countByAssignmentId(Long id);
 long countByAssignmentIdAndLateTrue(Long id);
 long countByAssignmentIdAndStatus(Long id,SubmissionStatus status);
}

