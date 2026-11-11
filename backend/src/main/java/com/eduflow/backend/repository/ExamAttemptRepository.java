package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt,Long>{
 Optional<ExamAttempt> findByExamIdAndStudentId(Long examId,Long studentId);
 List<ExamAttempt> findByExamId(Long id);
 @Query("select a.id from ExamAttempt a where a.status=com.eduflow.backend.entity.ExamAttemptStatus.IN_PROGRESS and a.expiresAt<=:now order by a.expiresAt,a.id")
 List<Long> expired(Instant now,Pageable pageable);
}
