package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface FeePaymentRepository extends JpaRepository<FeePayment,Long>, JpaSpecificationExecutor<FeePayment> {
    Optional<FeePayment> findByIdempotencyKey(String key);
    boolean existsByExternalReferenceKey(String key);
    boolean existsByFeeAccountId(Long id);
    List<FeePayment> findByFeeAccountIdOrderByCreatedAtDesc(Long accountId);
    @Query("select coalesce(sum(p.amount),0) from FeePayment p where p.feeAccount.id=:id and p.status=com.eduflow.backend.entity.FeePaymentStatus.SUCCESS")
    BigDecimal successfulTotal(Long id);
    @Query("select p.feeAccount.id, sum(p.amount) from FeePayment p where p.feeAccount.id in :ids and p.status=com.eduflow.backend.entity.FeePaymentStatus.SUCCESS group by p.feeAccount.id")
    List<Object[]> successfulTotals(Collection<Long> ids);
}
