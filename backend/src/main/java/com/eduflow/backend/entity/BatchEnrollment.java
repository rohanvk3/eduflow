package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name="batch_enrollments", uniqueConstraints=@UniqueConstraint(columnNames={"batch_id","student_id","enrollment_date"}), indexes=@Index(name="ix_enrollment_eligibility", columnList="batch_id,student_id,enrollment_date,withdrawal_date"))
public class BatchEnrollment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="batch_id", nullable=false)
    private Batch batch;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="student_id", nullable=false)
    private Student student;
    @Column(name="enrollment_date",nullable=false)
    private LocalDate enrollmentDate;
    @Column(name="withdrawal_date")
    private LocalDate withdrawalDate;
    
    private LocalDate completionDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;
    @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    public BatchEnrollment() {}
    @PrePersist void onCreate() { createdAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")); updatedAt=createdAt; }
    @PreUpdate void onUpdate() { updatedAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")); }
    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Batch getBatch() { return batch; }
    public void setBatch(Batch value) { batch=value; }
    public Student getStudent() { return student; }
    public void setStudent(Student value) { student=value; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate value) { enrollmentDate=value; }
    public LocalDate getWithdrawalDate() { return withdrawalDate; }
    public void setWithdrawalDate(LocalDate value) { withdrawalDate=value; }
    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate value) { completionDate=value; }
    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus value) { status=value; }
}

