package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface ExamResultRepository extends JpaRepository<ExamResult,Long>,JpaSpecificationExecutor<ExamResult>{
 Optional<ExamResult> findByExamIdAndStudentId(Long examId,Long studentId);
 List<ExamResult> findByExamId(Long id);
}
