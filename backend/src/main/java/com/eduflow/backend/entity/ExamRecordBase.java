package com.eduflow.backend.entity;
import jakarta.persistence.*;
import java.time.Instant;
@MappedSuperclass
public abstract class ExamRecordBase {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,updatable=false) private Instant createdAt;
 @Column(nullable=false) private Instant updatedAt;
 @PrePersist void created(){createdAt=Instant.now();updatedAt=createdAt;}
 @PreUpdate void updated(){updatedAt=Instant.now();}
 public Long getId(){return id;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}

