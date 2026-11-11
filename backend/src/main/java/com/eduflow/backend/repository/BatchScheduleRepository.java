package com.eduflow.backend.repository;
import com.eduflow.backend.entity.BatchSchedule;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BatchScheduleRepository extends JpaRepository<BatchSchedule,Long> {
    List<BatchSchedule> findByBatchIdOrderByDayOfWeekAscStartTimeAsc(Long batchId);
    List<BatchSchedule> findByTeacherIdAndActiveTrue(Long teacherId);
    List<BatchSchedule> findByBatchIdAndActiveTrue(Long batchId);
    boolean existsByBatchId(Long batchId);
}
