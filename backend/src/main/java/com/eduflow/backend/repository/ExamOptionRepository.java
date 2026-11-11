package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface ExamOptionRepository extends JpaRepository<ExamOption,Long>{
 List<ExamOption> findByQuestionIdOrderByDisplayOrder(Long id);
 void deleteByQuestionId(Long id);
}
