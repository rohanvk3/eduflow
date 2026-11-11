package com.eduflow.backend.dto;

import com.eduflow.backend.entity.AccountStatus;
import com.eduflow.backend.entity.AuditLog;
import com.eduflow.backend.entity.Role;
import com.eduflow.backend.entity.User;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;

public final class AdminDtos {
    public record Settings(@NotBlank @Size(max = 150) String instituteName,
                           @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,30}") String instituteCode,
                           @Email @Size(max = 254) String email,
                           @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$") String phoneNumber,
                           @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$") String alternatePhone,
                           @Size(max = 250) String addressLine1, @Size(max = 250) String addressLine2,
                           @Size(max = 100) String city, @Size(max = 100) String state,
                           @Size(max = 20) String postalCode, @Size(max = 100) String country,
                           @Size(max = 300) String website, @NotBlank String timezone, @NotBlank String currencyCode,
                           LocalDate academicYearStart, LocalDate academicYearEnd, @NotNull Long version,
                           boolean hasLogo) {
    }

    public record Account(Long id, String fullName, String email, Role role, AccountStatus status, Instant createdAt,
                          Instant resetRequestedAt) {
        public static Account from(User u) {
            return new Account(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getStatus(), u.getCreatedAt(), u.getResetRequestedAt());
        }
    }

    public record StatusRequest(@NotNull AccountStatus status) {
    }

    public record RoleRequest(@NotNull Role role) {
    }

    public record ProfileRequest(@NotBlank @Size(max = 100) String fullName) {
    }

    public record PasswordChange(@NotBlank String currentPassword,
                                 @NotBlank @Size(min = 12, max = 72) String newPassword) {
    }

    public record ResetRequest(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record ResetConfirm(@NotBlank @Size(max = 100) String token,
                               @NotBlank @Size(min = 12, max = 72) String newPassword) {
    }

    public record RecoveryLink(String resetToken, Instant expiresAt, String instruction) {
    }

    public record Branding(String instituteName, String instituteCode, boolean hasLogo) {
    }

    public record AuditView(Long id, Long actorUserId, String actorRole, String action, String entityType,
                            Long entityId, String description, String outcome, Instant timestamp, String requestId) {
        public static AuditView from(AuditLog a) {
            return new AuditView(a.id, a.actorUserId, a.actorRole, a.action, a.entityType, a.entityId, a.description, a.outcome, a.timestamp, a.requestId);
        }
    }
}
