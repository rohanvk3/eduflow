package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface SubmissionGradeHistoryRepository extends JpaRepository<SubmissionGradeHistory,Long>,JpaSpecificationExecutor<SubmissionGradeHistory>{
 List<SubmissionGradeHistory> findBySubmissionIdOrderByGradedAtDescIdDesc(Long id);
}

