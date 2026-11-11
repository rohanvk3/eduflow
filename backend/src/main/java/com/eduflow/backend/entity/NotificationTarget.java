package com.eduflow.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_targets", uniqueConstraints = @UniqueConstraint(name = "uk_notification_target", columnNames = {"job_id", "user_id"}), indexes = @Index(name = "ix_target_pending", columnList = "job_id,processed,id"))
public class NotificationTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private NotificationJob job;
    @Column(nullable = false)
    private Long userId;
    private Long studentId;
    @Column(nullable = false)
    private boolean processed = false;
    @Column(nullable = false)
    private boolean delivered = false;

    public NotificationJob getJob() {
        return job;
    }

    public void setJob(NotificationJob value) {
        job = value;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long value) {
        userId = value;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long value) {
        studentId = value;
    }

    public boolean getProcessed() {
        return processed;
    }

    public void setProcessed(boolean value) {
        processed = value;
    }

    public boolean getDelivered() {
        return delivered;
    }

    public void setDelivered(boolean value) {
        delivered = value;
    }
}

