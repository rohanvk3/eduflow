package com.eduflow.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_coordinators")
public class NotificationCoordinator {
    @Id
    @Column(length = 40)
    private String name;
    @Column(nullable = false)
    private long lastId = 0;

    public long getLastId() {
        return lastId;
    }

    public void setLastId(long value) {
        lastId = value;
    }

    public NotificationCoordinator() {
    }

    public NotificationCoordinator(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

