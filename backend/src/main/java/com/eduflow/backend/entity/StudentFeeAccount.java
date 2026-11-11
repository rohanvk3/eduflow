package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="student_fee_accounts",uniqueConstraints=@UniqueConstraint(name="uk_fee_account_enrollment",columnNames="enrollment_id"),indexes=@Index(name="ix_fee_account_student",columnList="student_id"))
public class StudentFeeAccount extends FinanceEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Student student;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private BatchEnrollment enrollment;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=true) private FeeStructure feeStructure;
    @Column(nullable=false,length=150) private String structureName;
    @Column(nullable=false) private LocalDate agreementDate;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal tuitionFee;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal admissionFee;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal materialFee;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal otherFee;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal originalAmount;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal discountAmount;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal finalPayableAmount;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private FeeAccountStatus status = FeeAccountStatus.ACTIVE;
    @Version private long version;
    public StudentFeeAccount() {}
    public Student getStudent() {return student;}
    public void setStudent(Student value) {student=value;}
    public BatchEnrollment getEnrollment() {return enrollment;}
    public void setEnrollment(BatchEnrollment value) {enrollment=value;}
    public FeeStructure getFeeStructure() {return feeStructure;}
    public void setFeeStructure(FeeStructure value) {feeStructure=value;}
    public String getStructureName() {return structureName;}
    public void setStructureName(String value) {structureName=value;}
    public LocalDate getAgreementDate() {return agreementDate;}
    public void setAgreementDate(LocalDate value) {agreementDate=value;}
    public BigDecimal getTuitionFee() {return tuitionFee;}
    public void setTuitionFee(BigDecimal value) {tuitionFee=value;}
    public BigDecimal getAdmissionFee() {return admissionFee;}
    public void setAdmissionFee(BigDecimal value) {admissionFee=value;}
    public BigDecimal getMaterialFee() {return materialFee;}
    public void setMaterialFee(BigDecimal value) {materialFee=value;}
    public BigDecimal getOtherFee() {return otherFee;}
    public void setOtherFee(BigDecimal value) {otherFee=value;}
    public BigDecimal getOriginalAmount() {return originalAmount;}
    public void setOriginalAmount(BigDecimal value) {originalAmount=value;}
    public BigDecimal getDiscountAmount() {return discountAmount;}
    public void setDiscountAmount(BigDecimal value) {discountAmount=value;}
    public BigDecimal getFinalPayableAmount() {return finalPayableAmount;}
    public void setFinalPayableAmount(BigDecimal value) {finalPayableAmount=value;}
    public FeeAccountStatus getStatus() {return status;}
    public void setStatus(FeeAccountStatus value) {status=value;}
    public long getVersion() {return version;}
}

