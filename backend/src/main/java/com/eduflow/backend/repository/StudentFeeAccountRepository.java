package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import org.springframework.data.jpa.repository.*;
public interface StudentFeeAccountRepository extends JpaRepository<StudentFeeAccount,Long>, JpaSpecificationExecutor<StudentFeeAccount> {
    @Override
    @EntityGraph(attributePaths={"student", "enrollment.batch.course", "feeStructure"})
    org.springframework.data.domain.Page<StudentFeeAccount> findAll(org.springframework.data.jpa.domain.Specification<StudentFeeAccount> spec, org.springframework.data.domain.Pageable pageable);
    boolean existsByEnrollmentId(Long enrollmentId);
    boolean existsByFeeStructureId(Long id);
}
