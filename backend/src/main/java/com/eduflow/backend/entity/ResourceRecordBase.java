package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.time.*;
@MappedSuperclass
public abstract class ResourceRecordBase {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
 @Column(nullable=false) private LocalDateTime updatedAt;
 @PrePersist void created(){createdAt=LocalDateTime.now(ZoneId.of("Asia/Kolkata"));updatedAt=createdAt;}
 @PreUpdate void updated(){updatedAt=LocalDateTime.now(ZoneId.of("Asia/Kolkata"));}
 public Long getId(){return id;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
 /** Mark an aggregate edit even when only an associated User row changes. */
 public void touch(){var now=LocalDateTime.now(ZoneId.of("Asia/Kolkata"));updatedAt=updatedAt==null||now.isAfter(updatedAt)?now:updatedAt.plusNanos(1000);}
}

