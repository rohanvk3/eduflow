package com.eduflow.backend.service;

import com.eduflow.backend.entity.AuditLog;
import com.eduflow.backend.repository.AuditLogRepository;
import com.eduflow.backend.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuditRecorder {
    final AuditLogRepository audits;
    final UserRepository users;

    public AuditRecorder(AuditLogRepository audits, UserRepository users) {
        this.audits = audits;
        this.users = users;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String type, Long id, String description) {
        save(action, type, id, description, "SUCCESS");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(String type) {
        save("ADMIN_REQUEST_REJECTED", type, null, "Sensitive request was rejected", "FAILURE");
    }

    private void save(String action, String type, Long id, String description, String outcome) {
        var entry = new AuditLog();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) users.findByEmail(auth.getName()).ifPresent(u -> {
            entry.actorUserId = u.getId();
            entry.actorRole = u.getRole().name();
        });
        entry.action = action;
        entry.entityType = type;
        entry.entityId = id;
        entry.description = description;
        entry.outcome = outcome;
        entry.timestamp = Instant.now();
        entry.requestId = org.slf4j.MDC.get("requestId");
        audits.saveAndFlush(entry);
    }
}
