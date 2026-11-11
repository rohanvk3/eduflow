package com.eduflow.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(name = "uk_notification_event", columnNames = {"recipient_id", "deduplication_key"}), indexes = {@Index(name = "ix_notification_inbox", columnList = "recipient_id,deleted_at,created_at"), @Index(name = "ix_notification_unread", columnList = "recipient_id,read_flag,deleted_at")})
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private User recipient;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 1000)
    private String message;
    @Column(length = 300)
    private String actionUrl;
    @Column(length = 40)
    private String relatedEntityType;
    private Long relatedEntityId;
    @Column(nullable = false)
    private boolean readFlag = false;
    private Instant readAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    private Instant expiresAt;
    private Instant deletedAt;
    @Column(length = 180)
    private String deduplicationKey;

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User value) {
        recipient = value;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType value) {
        type = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        title = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String value) {
        message = value;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String value) {
        actionUrl = value;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(String value) {
        relatedEntityType = value;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(Long value) {
        relatedEntityId = value;
    }

    public boolean getReadFlag() {
        return readFlag;
    }

    public void setReadFlag(boolean value) {
        readFlag = value;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant value) {
        readAt = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        createdAt = value;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant value) {
        expiresAt = value;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant value) {
        deletedAt = value;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }

    public void setDeduplicationKey(String value) {
        deduplicationKey = value;
    }
}

