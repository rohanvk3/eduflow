package com.eduflow.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 254)
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 60)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false,columnDefinition="varchar(20) default 'ACTIVE'")
    private AccountStatus status=AccountStatus.ACTIVE;
    @Column(nullable=false,columnDefinition="bigint default 0")
    private long securityVersion=0;
    private Instant resetRequestedAt;
    public AccountStatus getStatus(){return status;}
    public void setStatus(AccountStatus value){status=value;}
    public long getSecurityVersion(){return securityVersion;}
    public void revokeTokens(){securityVersion++;}
    public Instant getResetRequestedAt(){return resetRequestedAt;}
    public void setResetRequestedAt(Instant value){resetRequestedAt=value;}
    public void setRole(Role value){role=value;}
    public void setPassword(String value){password=value;}

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String fullName, String email, String password, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
