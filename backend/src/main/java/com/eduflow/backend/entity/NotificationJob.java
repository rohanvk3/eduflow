package com.eduflow.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notification_jobs", indexes = @Index(name = "ix_notification_job_pending", columnList = "complete,id"))
public class NotificationJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @Column(nullable = false, unique = true, length = 180)
    private String eventKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;
    @Column(nullable = false, length = 40)
    private String relatedType;
    @Column(nullable = false)
    private Long relatedId;
    @Column(length = 120)
    private String contextKey;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private boolean complete = false;
    @Column(nullable = false)
    private long recipientCount = 0;
    @Column(nullable = false)
    private long processedCount = 0;
    @Column(nullable = false)
    private long deliveredCount = 0;
    @Column(nullable = false)
    private long skippedCount = 0;

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String value) {
        eventKey = value;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType value) {
        type = value;
    }

    public String getRelatedType() {
        return relatedType;
    }

    public void setRelatedType(String value) {
        relatedType = value;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long value) {
        relatedId = value;
    }

    public String getContextKey() {
        return contextKey;
    }

    public void setContextKey(String value) {
        contextKey = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        createdAt = value;
    }

    public boolean getComplete() {
        return complete;
    }

    public void setComplete(boolean value) {
        complete = value;
    }

    public long getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(long value) {
        recipientCount = value;
    }

    public long getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(long value) {
        processedCount = value;
    }

    public long getDeliveredCount() {
        return deliveredCount;
    }

    public void setDeliveredCount(long value) {
        deliveredCount = value;
    }

    public long getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(long value) {
        skippedCount = value;
    }
}

