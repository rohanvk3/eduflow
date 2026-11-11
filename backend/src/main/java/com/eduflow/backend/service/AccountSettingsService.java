package com.eduflow.backend.service;

import com.eduflow.backend.dto.AdminDtos.*;
import com.eduflow.backend.entity.AccountStatus;
import com.eduflow.backend.entity.PasswordResetToken;
import com.eduflow.backend.entity.RecoveryThrottle;
import com.eduflow.backend.entity.User;
import com.eduflow.backend.exception.AcademicConflictException;
import com.eduflow.backend.exception.ResourceNotFoundException;
import com.eduflow.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AccountSettingsService {
    final UserRepository users;
    final AcademicAccess access;
    final EntityManager em;
    final PasswordEncoder encoder;
    final InstituteSettingsService institute;
    final AuditRecorder audit;
    final SecureRandom random = new SecureRandom();

    public AccountSettingsService(UserRepository users, AcademicAccess access, EntityManager em, PasswordEncoder encoder, InstituteSettingsService institute, AuditRecorder audit) {
        this.users = users;
        this.access = access;
        this.em = em;
        this.encoder = encoder;
        this.institute = institute;
        this.audit = audit;
    }

    User own(boolean lock) {
        var u = users.findByEmail(access.email()).orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (lock) { em.lock(u, LockModeType.PESSIMISTIC_WRITE); em.refresh(u); }
        return u;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Account profile() {
        return Account.from(own(false));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Account profile(ProfileRequest input) {
        var u = own(true);
        u.setFullName(input.fullName().strip());
        audit.record("ACCOUNT_PROFILE_UPDATED", "USER", u.getId(), "Account display name updated");
        return Account.from(u);
    }

    public static void strength(String password) {
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72 || !password.matches("(?s).*[A-Za-z].*") || !password.matches("(?s).*[0-9].*"))
            throw new IllegalArgumentException("Use at least 12 characters with a letter and a number; maximum 72 UTF-8 bytes");
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void password(PasswordChange input) {
        var u = own(true);
        if (!encoder.matches(input.currentPassword(), u.getPassword()))
            throw new IllegalArgumentException("Current password is incorrect");
        strength(input.newPassword());
        if (encoder.matches(input.newPassword(), u.getPassword()))
            throw new IllegalArgumentException("New password must differ from current password");
        u.setPassword(encoder.encode(input.newPassword()));
        u.revokeTokens();
        audit.record("ACCOUNT_PASSWORD_CHANGED", "USER", u.getId(), "Password changed; existing sessions revoked");
    }

    static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Transactional
    public void requestReset(ResetRequest input, String client) {
        institute.lock();
        Instant now = Instant.now();
        String key = hash(client);
        var throttle = em.find(RecoveryThrottle.class, key, LockModeType.PESSIMISTIC_WRITE);
        if (throttle != null && throttle.nextAllowedAt.isAfter(now)) return;
        if (throttle == null) {
            throttle = new RecoveryThrottle();
            throttle.clientHash = key;
            throttle.nextAllowedAt = now.plusSeconds(60);
            em.persist(throttle);
        }
        throttle.nextAllowedAt = now.plusSeconds(60);
        users.findForAuthentication(input.email().strip().toLowerCase(Locale.ROOT)).ifPresent(u -> {
            u.setResetRequestedAt(now);
        });
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public RecoveryLink issue(Long id) {
        institute.lock();
        var u = em.find(User.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (u == null) throw new ResourceNotFoundException("User not found");
        if (u.getStatus() != AccountStatus.ACTIVE)
            throw new AcademicConflictException("Activate/unlock the account before assisting recovery");
        long recent = em.createQuery("select count(t) from PasswordResetToken t where t.user=:user and t.expiresAt>:limit", Long.class).setParameter("user", u).setParameter("limit", Instant.now().plusSeconds(14 * 60)).getSingleResult();
        if (recent > 0) throw new AcademicConflictException("Wait one minute before issuing another recovery link");
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var t = new PasswordResetToken();
        t.tokenHash = hash(token);
        t.user = u;
        t.securityVersion = u.getSecurityVersion();
        t.expiresAt = Instant.now().plusSeconds(15 * 60);
        em.persist(t);
        audit.record("PASSWORD_RECOVERY_ISSUED", "USER", id, "Single-use recovery issued after administrator verification; no email sent");
        return new RecoveryLink(token, t.expiresAt, "Verify the account holder's identity. Deliver this single-use link privately. No email was sent. Never paste it into audit notes or logs.");
    }

    @Transactional
    public void confirm(ResetConfirm input) {
        strength(input.newPassword());
        institute.lock();
        var t = em.find(PasswordResetToken.class, hash(input.token()), LockModeType.PESSIMISTIC_WRITE);
        if (t == null || t.usedAt != null || !t.expiresAt.isAfter(Instant.now()))
            throw new IllegalArgumentException("Recovery link is invalid or expired");
        var u = t.user;
        em.lock(u, LockModeType.PESSIMISTIC_WRITE);
        em.refresh(u);
        if(u.getStatus()!=AccountStatus.ACTIVE||t.securityVersion!=u.getSecurityVersion())
            throw new IllegalArgumentException("Recovery link is invalid or expired");
        u.setPassword(encoder.encode(input.newPassword()));
        u.revokeTokens();
        u.setResetRequestedAt(null);
        t.usedAt = Instant.now();
        audit.record("PASSWORD_RECOVERY_COMPLETED", "USER", u.getId(), "Password recovered; existing sessions and recovery tokens revoked");
    }
}
