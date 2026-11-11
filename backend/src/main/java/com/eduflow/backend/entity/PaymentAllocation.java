package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="payment_allocations",uniqueConstraints=@UniqueConstraint(name="uk_payment_installment",columnNames={"payment_id","installment_id"}))
public class PaymentAllocation extends FinanceEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private FeePayment payment;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private FeeInstallment installment;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal amount;
    public PaymentAllocation() {}
    public FeePayment getPayment() {return payment;}
    public void setPayment(FeePayment value) {payment=value;}
    public FeeInstallment getInstallment() {return installment;}
    public void setInstallment(FeeInstallment value) {installment=value;}
    public BigDecimal getAmount() {return amount;}
    public void setAmount(BigDecimal value) {amount=value;}
}

