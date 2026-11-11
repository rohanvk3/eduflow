package com.eduflow.backend.repository;
import com.eduflow.backend.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentVoidAuditRepository extends JpaRepository<PaymentVoidAudit,Long> {
    Optional<PaymentVoidAudit> findByPaymentId(Long id);
}
