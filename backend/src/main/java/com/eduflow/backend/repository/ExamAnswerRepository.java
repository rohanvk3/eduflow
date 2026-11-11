package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer,Long>{
 List<ExamAnswer> findByAttemptId(Long id);
}
