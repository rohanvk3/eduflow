package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name="batch_teacher_assignments", uniqueConstraints=@UniqueConstraint(columnNames={"batch_id","teacher_id"}))
public class BatchTeacherAssignment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="batch_id", nullable=false)
    private Batch batch;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="teacher_id", nullable=false)
    private Teacher teacher;
    @Column(nullable=false)
    private boolean active = true;
    @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    public BatchTeacherAssignment() {}
    @PrePersist void onCreate() { createdAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")); updatedAt=createdAt; }
    @PreUpdate void onUpdate() { updatedAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")); }
    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Batch getBatch() { return batch; }
    public void setBatch(Batch value) { batch=value; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher value) { teacher=value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active=value; }
}

