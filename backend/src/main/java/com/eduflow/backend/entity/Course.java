package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name="courses")
public class Course {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, length=40)
    private String code;
    @Column(nullable=false, length=150)
    private String name;
    @Column(length=2000)
    private String description;
    @Column(nullable=false)
    private Integer durationWeeks;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private StudentStatus status = StudentStatus.ACTIVE;
    @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    public Course() {}
    @PrePersist void onCreate() { createdAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")); updatedAt=createdAt; }
    @PreUpdate void onUpdate() { updatedAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")); }
    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCode() { return code; }
    public void setCode(String value) { code=value; }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description=value; }
    public Integer getDurationWeeks() { return durationWeeks; }
    public void setDurationWeeks(Integer value) { durationWeeks=value; }
    public StudentStatus getStatus() { return status; }
    public void setStatus(StudentStatus value) { status=value; }
}

