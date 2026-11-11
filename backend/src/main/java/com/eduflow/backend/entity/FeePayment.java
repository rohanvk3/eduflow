package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="fee_payments",uniqueConstraints={@UniqueConstraint(name="uk_fee_receipt",columnNames="receipt_number"),@UniqueConstraint(name="uk_fee_idempotency",columnNames="idempotency_key"),@UniqueConstraint(name="uk_fee_external_reference",columnNames="external_reference_key")},indexes=@Index(name="ix_fee_payment_date_status",columnList="payment_date,status"))
public class FeePayment extends FinanceEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private StudentFeeAccount feeAccount;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal amount;
    @Column(nullable=false) private LocalDate paymentDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentMethod paymentMethod;
    @Column(length=150) private String transactionReference;
    @Column(length=180) private String externalReferenceKey;
    @Column(nullable=false,length=60) private String receiptNumber;
    @Column(nullable=false,length=100) private String idempotencyKey;
    @Column(nullable=false,length=64) private String requestHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private FeePaymentStatus status = FeePaymentStatus.SUCCESS;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private User recordedBy;
    @Column(length=1000) private String remarks;
    public FeePayment() {}
    public StudentFeeAccount getFeeAccount() {return feeAccount;}
    public void setFeeAccount(StudentFeeAccount value) {feeAccount=value;}
    public BigDecimal getAmount() {return amount;}
    public void setAmount(BigDecimal value) {amount=value;}
    public LocalDate getPaymentDate() {return paymentDate;}
    public void setPaymentDate(LocalDate value) {paymentDate=value;}
    public PaymentMethod getPaymentMethod() {return paymentMethod;}
    public void setPaymentMethod(PaymentMethod value) {paymentMethod=value;}
    public String getTransactionReference() {return transactionReference;}
    public void setTransactionReference(String value) {transactionReference=value;}
    public String getExternalReferenceKey() {return externalReferenceKey;}
    public void setExternalReferenceKey(String value) {externalReferenceKey=value;}
    public String getReceiptNumber() {return receiptNumber;}
    public void setReceiptNumber(String value) {receiptNumber=value;}
    public String getIdempotencyKey() {return idempotencyKey;}
    public void setIdempotencyKey(String value) {idempotencyKey=value;}
    public String getRequestHash() {return requestHash;}
    public void setRequestHash(String value) {requestHash=value;}
    public FeePaymentStatus getStatus() {return status;}
    public void setStatus(FeePaymentStatus value) {status=value;}
    public User getRecordedBy() {return recordedBy;}
    public void setRecordedBy(User value) {recordedBy=value;}
    public String getRemarks() {return remarks;}
    public void setRemarks(String value) {remarks=value;}
}

