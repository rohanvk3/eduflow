package com.eduflow.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false, unique = true)
    private User user;
    @Column(nullable = false)
    private boolean announcementsEnabled = true;
    @Column(nullable = false)
    private boolean examNotificationsEnabled = true;
    @Column(nullable = false)
    private boolean assignmentNotificationsEnabled = true;
    @Column(nullable = false)
    private boolean feeNotificationsEnabled = true;
    @Column(nullable = false)
    private boolean materialNotificationsEnabled = true;
    @Column(nullable = false)
    private boolean attendanceNotificationsEnabled = false;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    public User getUser() {
        return user;
    }

    public void setUser(User value) {
        user = value;
    }

    public boolean getAnnouncementsEnabled() {
        return announcementsEnabled;
    }

    public void setAnnouncementsEnabled(boolean value) {
        announcementsEnabled = value;
    }

    public boolean getExamNotificationsEnabled() {
        return examNotificationsEnabled;
    }

    public void setExamNotificationsEnabled(boolean value) {
        examNotificationsEnabled = value;
    }

    public boolean getAssignmentNotificationsEnabled() {
        return assignmentNotificationsEnabled;
    }

    public void setAssignmentNotificationsEnabled(boolean value) {
        assignmentNotificationsEnabled = value;
    }

    public boolean getFeeNotificationsEnabled() {
        return feeNotificationsEnabled;
    }

    public void setFeeNotificationsEnabled(boolean value) {
        feeNotificationsEnabled = value;
    }

    public boolean getMaterialNotificationsEnabled() {
        return materialNotificationsEnabled;
    }

    public void setMaterialNotificationsEnabled(boolean value) {
        materialNotificationsEnabled = value;
    }

    public boolean getAttendanceNotificationsEnabled() {
        return attendanceNotificationsEnabled;
    }

    public void setAttendanceNotificationsEnabled(boolean value) {
        attendanceNotificationsEnabled = value;
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
}

