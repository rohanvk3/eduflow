package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface AssignmentRecipientRepository extends JpaRepository<AssignmentRecipient,Long>,JpaSpecificationExecutor<AssignmentRecipient>{
 long countByAssignmentId(Long id);
 boolean existsByAssignmentIdAndStudentId(Long assignmentId,Long studentId);
 List<AssignmentRecipient> findByAssignmentIdOrderByStudentFullName(Long id);
}

