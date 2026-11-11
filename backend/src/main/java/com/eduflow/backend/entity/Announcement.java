package com.eduflow.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "announcements", indexes = {@Index(name = "ix_announcement_schedule", columnList = "status,publish_at"), @Index(name = "ix_announcement_scope", columnList = "batch_id,status")})
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementAudience audienceType;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role targetRole;
    @ManyToOne(fetch = FetchType.LAZY)
    private Course course;
    @ManyToOne(fetch = FetchType.LAZY)
    private Batch batch;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private User createdBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;
    private Instant publishAt;
    private Instant publishedAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    private Long deliveryJobId;
    @Version
    private long version;
    @Column(length = 250)
    private String publicationIssue;

    public String getPublicationIssue() {
        return publicationIssue;
    }

    public void setPublicationIssue(String value) {
        publicationIssue = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        title = value;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String value) {
        content = value;
    }

    public AnnouncementAudience getAudienceType() {
        return audienceType;
    }

    public void setAudienceType(AnnouncementAudience value) {
        audienceType = value;
    }

    public Role getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(Role value) {
        targetRole = value;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course value) {
        course = value;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch value) {
        batch = value;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User value) {
        createdBy = value;
    }

    public AnnouncementStatus getStatus() {
        return status;
    }

    public void setStatus(AnnouncementStatus value) {
        status = value;
    }

    public Instant getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(Instant value) {
        publishAt = value;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant value) {
        publishedAt = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        createdAt = value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant value) {
        updatedAt = value;
    }

    public Long getDeliveryJobId() {
        return deliveryJobId;
    }

    public void setDeliveryJobId(Long value) {
        deliveryJobId = value;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long value) {
        version = value;
    }
}

