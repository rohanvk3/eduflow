package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
public interface BatchRepository extends JpaRepository<Batch,Long>, JpaSpecificationExecutor<Batch> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from Batch b where b.id=:id")
    Optional<Batch> findLockedById(Long id);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    long countByStatus(BatchStatus status);
}
