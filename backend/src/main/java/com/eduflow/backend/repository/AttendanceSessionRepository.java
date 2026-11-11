package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession,Long>,JpaSpecificationExecutor<AttendanceSession> {
    @Query("select s.batch.id from AttendanceSession s where s.id=:id") Optional<Long> findBatchIdById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from AttendanceSession s where s.id=:id") Optional<AttendanceSession> findLockedById(Long id);
    boolean existsByBatchIdAndSessionDateAndOccurrenceKey(Long batchId,LocalDate date,String occurrenceKey);
}
