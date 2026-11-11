package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment,Long>,JpaSpecificationExecutor<SubmissionAttachment>{
 List<SubmissionAttachment> findBySubmissionIdAndActiveTrueOrderById(Long id);
 List<SubmissionAttachment> findBySubmissionIdOrderById(Long id);
}

