package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachment,Long>,JpaSpecificationExecutor<AssignmentAttachment>{
 List<AssignmentAttachment> findByAssignmentIdOrderById(Long id);
}

