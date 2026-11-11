package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface TeacherRepository extends JpaRepository<Teacher, Long>, JpaSpecificationExecutor<Teacher> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Teacher t where t.id=:id")
    Optional<Teacher> findLockedById(Long id);
    Optional<Teacher> findByEmployeeId(String employeeId);
    Optional<Teacher> findByEmail(String email);
    Optional<Teacher> findByUserEmail(String email);
    boolean existsByEmployeeIdIgnoreCaseAndIdNot(String employeeId, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByUserIdAndIdNot(Long userId, Long id);
    long countByStatus(StudentStatus status);
}
