package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface SubmissionRevisionRepository extends JpaRepository<SubmissionRevision,Long>,JpaSpecificationExecutor<SubmissionRevision>{
 List<SubmissionRevision> findBySubmissionIdOrderByRevisionNumberDesc(Long id);
}

