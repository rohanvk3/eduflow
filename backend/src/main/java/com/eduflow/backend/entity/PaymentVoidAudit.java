package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="payment_void_audits",uniqueConstraints=@UniqueConstraint(name="uk_payment_void",columnNames="payment_id"))
public class PaymentVoidAudit extends FinanceEntity {
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private FeePayment payment;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User changedBy;
    @Column(nullable=false,length=1000) private String reason;
    @Column(nullable=false,length=20) private String previousStatus = "SUCCESS";
    @Column(nullable=false,length=20) private String newStatus = "VOIDED";
    @Column(nullable=false,precision=14,scale=2) private BigDecimal previousAmount;
    public PaymentVoidAudit() {}
    public FeePayment getPayment() {return payment;}
    public void setPayment(FeePayment value) {payment=value;}
    public User getChangedBy() {return changedBy;}
    public void setChangedBy(User value) {changedBy=value;}
    public String getReason() {return reason;}
    public void setReason(String value) {reason=value;}
    public String getPreviousStatus() {return previousStatus;}
    public void setPreviousStatus(String value) {previousStatus=value;}
    public String getNewStatus() {return newStatus;}
    public void setNewStatus(String value) {newStatus=value;}
    public BigDecimal getPreviousAmount() {return previousAmount;}
    public void setPreviousAmount(BigDecimal value) {previousAmount=value;}
}

