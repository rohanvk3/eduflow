package com.eduflow.backend.repository;
import com.eduflow.backend.entity.BatchTeacherAssignment;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BatchTeacherAssignmentRepository extends JpaRepository<BatchTeacherAssignment,Long> {
    Optional<BatchTeacherAssignment> findByBatchIdAndTeacherId(Long batchId, Long teacherId);
    List<BatchTeacherAssignment> findByBatchIdOrderById(Long batchId);
    boolean existsByBatchIdAndTeacherUserEmailAndActiveTrue(Long batchId,String email);
    boolean existsByBatchIdAndTeacherIdAndActiveTrue(Long batchId,Long teacherId);
}
