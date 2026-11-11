package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion,Long>{
 List<ExamQuestion> findByExamIdOrderByDisplayOrderAscIdAsc(Long id);
}
