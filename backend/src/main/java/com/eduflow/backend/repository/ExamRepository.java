package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import org.springframework.data.jpa.repository.*;
public interface ExamRepository extends JpaRepository<Exam,Long>,JpaSpecificationExecutor<Exam>{
 boolean existsByExamCodeIgnoreCaseAndIdNot(String code,Long id);
}
