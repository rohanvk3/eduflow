package com.eduflow.backend.repository;
import com.eduflow.backend.entity.AttendanceRecord;
import java.util.List;
import org.springframework.data.jpa.repository.*;
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord,Long>,JpaSpecificationExecutor<AttendanceRecord> {
    List<AttendanceRecord> findBySessionIdOrderByStudentFullNameAscStudentIdAsc(Long sessionId);
    long countBySessionId(Long sessionId);
    @Modifying
    @Query("delete from AttendanceRecord r where r.session.id=:sessionId")
    void deleteForSession(Long sessionId);
}
