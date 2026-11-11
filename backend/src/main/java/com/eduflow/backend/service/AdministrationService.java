package com.eduflow.backend.service;

import com.eduflow.backend.dto.AdminDtos.Account;
import com.eduflow.backend.dto.AdminDtos.AuditView;
import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.entity.AccountStatus;
import com.eduflow.backend.entity.Role;
import com.eduflow.backend.entity.User;
import com.eduflow.backend.exception.AcademicConflictException;
import com.eduflow.backend.exception.ResourceNotFoundException;
import com.eduflow.backend.repository.AuditLogRepository;
import com.eduflow.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdministrationService {
    final UserRepository users;
    final AuditLogRepository logs;
    final EntityManager em;
    final AcademicAccess access;
    final InstituteSettingsService institute;
    final AuditRecorder audit;

    public AdministrationService(UserRepository users, AuditLogRepository logs, EntityManager em, AcademicAccess access, InstituteSettingsService institute, AuditRecorder audit) {
        this.users = users;
        this.logs = logs;
        this.em = em;
        this.access = access;
        this.institute = institute;
        this.audit = audit;
    }

    User user(Long id, boolean lock) {
        var u = em.find(User.class, id, lock ? LockModeType.PESSIMISTIC_WRITE : LockModeType.NONE);
        if (u == null) throw new ResourceNotFoundException("User not found");
        return u;
    }

    public PageResponse<Account> users(String search, Role role, AccountStatus status, int page, int size) {
        CourseService.validatePage(page, size, search);
        String pattern = CourseService.pattern(search);
        return PageResponse.from(users.findAll((r, q, cb) -> {
            var p = cb.or(cb.like(cb.lower(r.get("fullName")), pattern, '\\'), cb.like(cb.lower(r.get("email")), pattern, '\\'));
            if (role != null) p = cb.and(p, cb.equal(r.get("role"), role));
            if (status != null) p = cb.and(p, cb.equal(r.get("status"), status));
            return p;
        }, PageRequest.of(page, size, Sort.by("id"))).map(Account::from));
    }

    public Account get(Long id) {
        return Account.from(user(id, false));
    }

    void guard(User u, Role role, AccountStatus status) {
        var actor = users.findByEmail(access.email()).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Administrator unavailable"));
        em.refresh(actor);
        if (actor.getStatus() != AccountStatus.ACTIVE || actor.getRole() != Role.ADMIN)
            throw new org.springframework.security.access.AccessDeniedException("Administrator no longer active");
        boolean removes = u.getRole() == Role.ADMIN && u.getStatus() == AccountStatus.ACTIVE && (role != Role.ADMIN || status != AccountStatus.ACTIVE);
        if (u.getEmail().equals(access.email()) && (role != u.getRole() || status != AccountStatus.ACTIVE))
            throw new AcademicConflictException("You cannot change your own role, deactivate, or lock your account");
        if (removes && em.createQuery("select count(u) from User u where u.role=com.eduflow.backend.entity.Role.ADMIN and u.status=com.eduflow.backend.entity.AccountStatus.ACTIVE", Long.class).getSingleResult() <= 1)
            throw new AcademicConflictException("The last active administrator must be preserved");
    }

    @Transactional
    public Account status(Long id, AccountStatus status) {
        institute.lock();
        var u = user(id, true);
        guard(u, u.getRole(), status);
        if (u.getStatus() != status) {
            u.setStatus(status);
            u.revokeTokens();
            audit.record("USER_STATUS_CHANGED", "USER", id, "Account status changed to " + status);
        }
        return Account.from(u);
    }

    @Transactional
    public Account role(Long id, Role role) {
        institute.lock();
        var u = user(id, true);
        guard(u, role, u.getStatus());
        if (u.getRole() != role) {
            for (var entry : Map.of("Student", Role.STUDENT, "Teacher", Role.TEACHER, "ParentProfile", Role.PARENT).entrySet())
                if (entry.getValue() != role && em.createQuery("select count(p) from " + entry.getKey() + " p where p.user.id=:id", Long.class).setParameter("id", id).getSingleResult() > 0)
                    throw new AcademicConflictException("Role change conflicts with an existing linked profile");
            u.setRole(role);
            u.revokeTokens();
            audit.record("USER_ROLE_CHANGED", "USER", id, "Role changed to " + role);
        }
        return Account.from(u);
    }

    public PageResponse<AuditView> logs(String search, String action, Long actorUserId, String entityType, String outcome, LocalDate from, LocalDate to, int page, int size) {
        CourseService.validatePage(page, size, search);
        var d = AttendanceReportService.range(from, to);
        String ptn = CourseService.pattern(search);
        return PageResponse.from(logs.findAll((r, q, cb) -> {
            var p = cb.and(cb.greaterThanOrEqualTo(r.get("timestamp"), d[0].atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant()), cb.lessThan(r.get("timestamp"), d[1].plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant()), cb.like(cb.lower(r.get("description")), ptn, '\\'));
            if (action != null) p = cb.and(p, cb.equal(r.get("action"), action));
            if (actorUserId != null) p = cb.and(p, cb.equal(r.get("actorUserId"), actorUserId));
            if (entityType != null) p = cb.and(p, cb.equal(r.get("entityType"), entityType));
            if (outcome != null) p = cb.and(p, cb.equal(r.get("outcome"), outcome));
            return p;
        }, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp", "id"))).map(AuditView::from));
    }

    public AuditView log(Long id) {
        return AuditView.from(logs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Audit event not found")));
    }

    public Map<String, Object> center() {
        var m = new LinkedHashMap<String, Object>();
        m.put("institute", institute.get());
        var counts = new LinkedHashMap<String, Long>();
        for (var e : Map.of("activeUsers", "User", "activeStudents", "Student", "activeTeachers", "Teacher", "activeParents", "ParentProfile", "activeCourses", "Course").entrySet()) {
            String value = e.getValue().equals("User") ? "com.eduflow.backend.entity.AccountStatus.ACTIVE" : "com.eduflow.backend.entity.StudentStatus.ACTIVE";
            counts.put(e.getKey(), em.createQuery("select count(e) from " + e.getValue() + " e where e.status=" + value, Long.class).getSingleResult());
        }
        counts.put("activeBatches", em.createQuery("select count(b) from Batch b where b.status=com.eduflow.backend.entity.BatchStatus.ACTIVE", Long.class).getSingleResult());
        m.put("counts", counts);
        m.put("recentAuditEvents", logs("", null, null, null, null, null, null, 0, 8).content());
        m.put("security", List.of("Bearer JWT with live status and security-version validation", "Role and ownership checks enforced by existing services", "Private file storage; PNG/JPEG logos validated", "Password recovery requires verified administrator handoff; no email provider"));
        var warnings = new ArrayList<String>();
        for (String entity : List.of("Student", "Teacher", "ParentProfile")) {
            long count = em.createQuery("select count(p) from " + entity + " p where p.user is null", Long.class).getSingleResult();
            if (count > 0) warnings.add(count + " " + entity + " profiles have no linked account");
        }
        m.put("integrityWarnings", warnings);
        return m;
    }
}
