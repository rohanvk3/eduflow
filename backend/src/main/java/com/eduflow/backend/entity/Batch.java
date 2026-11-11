package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name="batches")
public class Batch {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, length=40)
    private String code;
    @Column(nullable=false, length=150)
    private String name;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="course_id", nullable=false)
    private Course course;
    @Column(nullable=false)
    private Integer capacity;
    @Column(nullable=false)
    private LocalDate startDate;
    @Column(nullable=false)
    private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private BatchStatus status = BatchStatus.PLANNED;
    @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    public Batch() {}
    @PrePersist void onCreate() { createdAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")); updatedAt=createdAt; }
    @PreUpdate void onUpdate() { updatedAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")); }
    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCode() { return code; }
    public void setCode(String value) { code=value; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public Course getCourse() { return course; }
    public void setCourse(Course value) { course=value; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer value) { capacity=value; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate value) { startDate=value; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate value) { endDate=value; }
    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus value) { status=value; }
}

