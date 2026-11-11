package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FeeInstallmentRepository extends JpaRepository<FeeInstallment,Long> {
    List<FeeInstallment> findByFeeAccountIdOrderByInstallmentNumber(Long accountId);
    boolean existsByFeeAccountId(Long accountId);
    @org.springframework.data.jpa.repository.Query(value="""
        select i.fee_account_id, sum(i.amount - coalesce(a.paid, 0))
        from fee_installments i
        left join (
          select a.installment_id, sum(a.amount) paid
          from payment_allocations a join fee_payments p on p.id=a.payment_id
          where p.status='SUCCESS' and p.fee_account_id in (:ids)
          group by a.installment_id
        ) a on a.installment_id=i.id
        where i.fee_account_id in (:ids) and i.due_date < :today
        group by i.fee_account_id
        """,nativeQuery=true)
    List<Object[]> overdueTotals(Collection<Long> ids, java.time.LocalDate today);
}
