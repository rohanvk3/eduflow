package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="fee_structures",indexes=@Index(name="ix_fee_structure_course_dates",columnList="course_id,effective_from,effective_to"))
public class FeeStructure extends FinanceEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false) private Course course;
    @Column(nullable=false,length=150) private String name;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal tuitionFee;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal admissionFee;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal materialFee;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal otherFee;
    @Column(nullable=false,precision=14,scale=2) private BigDecimal totalFee;
    @Column(nullable=false) private LocalDate effectiveFrom;
     private LocalDate effectiveTo;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private StudentStatus status = StudentStatus.ACTIVE;
    public FeeStructure() {}
    public Course getCourse() {return course;}
    public void setCourse(Course value) {course=value;}
    public String getName() {return name;}
    public void setName(String value) {name=value;}
    public BigDecimal getTuitionFee() {return tuitionFee;}
    public void setTuitionFee(BigDecimal value) {tuitionFee=value;}
    public BigDecimal getAdmissionFee() {return admissionFee;}
    public void setAdmissionFee(BigDecimal value) {admissionFee=value;}
    public BigDecimal getMaterialFee() {return materialFee;}
    public void setMaterialFee(BigDecimal value) {materialFee=value;}
    public BigDecimal getOtherFee() {return otherFee;}
    public void setOtherFee(BigDecimal value) {otherFee=value;}
    public BigDecimal getTotalFee() {return totalFee;}
    public void setTotalFee(BigDecimal value) {totalFee=value;}
    public LocalDate getEffectiveFrom() {return effectiveFrom;}
    public void setEffectiveFrom(LocalDate value) {effectiveFrom=value;}
    public LocalDate getEffectiveTo() {return effectiveTo;}
    public void setEffectiveTo(LocalDate value) {effectiveTo=value;}
    public StudentStatus getStatus() {return status;}
    public void setStatus(StudentStatus value) {status=value;}
}

