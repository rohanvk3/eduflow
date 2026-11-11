package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="fee_installments",uniqueConstraints=@UniqueConstraint(name="uk_fee_installment_number",columnNames={"fee_account_id","installment_number"}),indexes=@Index(name="ix_fee_installment_due",columnList="due_date"))
public class FeeInstallment extends FinanceEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private StudentFeeAccount feeAccount;
    @Column(nullable=false) private Integer installmentNumber;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal amount;
    @Column(nullable=false) private LocalDate dueDate;
    public FeeInstallment() {}
    public StudentFeeAccount getFeeAccount() {return feeAccount;}
    public void setFeeAccount(StudentFeeAccount value) {feeAccount=value;}
    public Integer getInstallmentNumber() {return installmentNumber;}
    public void setInstallmentNumber(Integer value) {installmentNumber=value;}
    public BigDecimal getAmount() {return amount;}
    public void setAmount(BigDecimal value) {amount=value;}
    public LocalDate getDueDate() {return dueDate;}
    public void setDueDate(LocalDate value) {dueDate=value;}
}

