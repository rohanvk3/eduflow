package com.eduflow.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@org.hibernate.annotations.Immutable
@Table(name = "audit_logs", indexes = {@Index(name = "ix_audit_time", columnList = "event_time,id"), @Index(name = "ix_audit_actor", columnList = "actor_user_id,event_time"), @Index(name = "ix_audit_action", columnList = "action,event_time")})
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long actorUserId;
    @Column(length = 20)
    public String actorRole;
    @Column(nullable = false, length = 80)
    public String action;
    @Column(nullable = false, length = 60)
    public String entityType;
    public Long entityId;
    @Column(nullable = false, length = 500)
    public String description;
    @Column(nullable = false, length = 20)
    public String outcome;
    @Column(name = "event_time", nullable = false)
    public Instant timestamp;
    @Column(length = 36)
    public String requestId;
}
