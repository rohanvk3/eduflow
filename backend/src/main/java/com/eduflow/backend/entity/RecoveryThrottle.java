package com.eduflow.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "recovery_throttles")
public class RecoveryThrottle {
    @Id
    @Column(length = 64)
    public String clientHash;
    @Column(nullable = false)
    public Instant nextAllowedAt;
}
