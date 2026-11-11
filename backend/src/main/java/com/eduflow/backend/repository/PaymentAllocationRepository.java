package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation,Long> {
    List<PaymentAllocation> findByPaymentIdOrderByInstallmentInstallmentNumber(Long id);
    @Query("select coalesce(sum(a.amount),0) from PaymentAllocation a where a.installment.id=:id and a.payment.status=com.eduflow.backend.entity.FeePaymentStatus.SUCCESS")
    BigDecimal successfulTotal(Long id);
}
