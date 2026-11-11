package com.eduflow.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens", indexes = @Index(name = "ix_reset_user", columnList = "user_id,expires_at"))
public class PasswordResetToken {
    @Id
    @Column(length = 64)
    public String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public User user;
    @Column(nullable = false)
    public Instant expiresAt;
    public Instant usedAt;
    @Column(nullable = false)
    public long securityVersion;
}
