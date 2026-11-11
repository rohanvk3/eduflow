package com.eduflow.backend.entity;

import jakarta.persistence.*;
import java.time.*;

@MappedSuperclass
public abstract class FinanceEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    @PrePersist protected void created() { createdAt=LocalDateTime.now(ZoneId.of("Asia/Kolkata"));updatedAt=createdAt; }
    @PreUpdate protected void updated() { updatedAt=LocalDateTime.now(ZoneId.of("Asia/Kolkata")); }
    public Long getId() {return id;}
    public LocalDateTime getCreatedAt() {return createdAt;}
    public LocalDateTime getUpdatedAt() {return updatedAt;}
}

