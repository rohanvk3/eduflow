package com.eduflow.backend.service;

import com.eduflow.backend.dto.NotificationDtos.NotificationView;
import com.eduflow.backend.dto.NotificationDtos.PreferenceRequest;
import com.eduflow.backend.dto.NotificationDtos.Preferences;
import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.entity.Notification;
import com.eduflow.backend.entity.NotificationPreference;
import com.eduflow.backend.entity.NotificationType;
import com.eduflow.backend.entity.User;
import com.eduflow.backend.repository.NotificationPreferenceRepository;
import com.eduflow.backend.repository.NotificationRepository;
import com.eduflow.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@PreAuthorize("isAuthenticated()")
public class NotificationService {
    final NotificationRepository notifications;
    final NotificationPreferenceRepository preferences;
    final UserRepository users;
    final AcademicAccess auth;
    final EntityManager em;
    final Clock clock;

    public NotificationService(NotificationRepository notifications, NotificationPreferenceRepository preferences, UserRepository users, AcademicAccess auth, EntityManager em, @Qualifier("notificationClock") Clock clock) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.users = users;
        this.auth = auth;
        this.em = em;
        this.clock = clock;
    }

    User own() {
        return users.findByEmail(auth.email()).orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    }

    public PageResponse<NotificationView> list(NotificationType type, String readStatus, LocalDate from, LocalDate to, String search, int page, int size) {
        CourseService.validatePage(page, size, search);
        if (!Set.of("ALL", "READ", "UNREAD").contains(readStatus))
            throw new IllegalArgumentException("readStatus must be ALL, READ or UNREAD");
        LocalDate[] dates = from == null && to == null ? null : AttendanceReportService.range(from, to);
        Long uid = own().getId();
        Instant now = clock.instant();
        String pattern = CourseService.pattern(search);
        return PageResponse.from(notifications.findAll((r, q, cb) -> {
            var p = cb.and(cb.equal(r.get("recipient").get("id"), uid), cb.isNull(r.get("deletedAt")), cb.or(cb.isNull(r.get("expiresAt")), cb.greaterThan(r.get("expiresAt"), now)), cb.or(cb.like(cb.lower(r.get("title")), pattern, '\\'), cb.like(cb.lower(r.get("message")), pattern, '\\')));
            if (type != null) p = cb.and(p, cb.equal(r.get("type"), type));
            if (!readStatus.equals("ALL")) p = cb.and(p, cb.equal(r.get("readFlag"), readStatus.equals("READ")));
            if (dates != null)
                p = cb.and(p, cb.greaterThanOrEqualTo(r.get("createdAt"), dates[0].atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant()), cb.lessThan(r.get("createdAt"), dates[1].plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant()));
            return p;
        }, FinanceService.paging(page, size, "createdAt,desc", Set.of("createdAt"))).map(this::view));
    }

    public long unread() {
        Long uid = own().getId();
        Instant now = clock.instant();
        return notifications.count((r, q, cb) -> cb.and(cb.equal(r.get("recipient").get("id"), uid), cb.isFalse(r.get("readFlag")), cb.isNull(r.get("deletedAt")), cb.or(cb.isNull(r.get("expiresAt")), cb.greaterThan(r.get("expiresAt"), now))));
    }

    private Notification mine(Long id) {
        var n = em.find(Notification.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (n == null || !n.getRecipient().getId().equals(own().getId()) || n.getDeletedAt() != null)
            throw new AccessDeniedException("Notification access denied");
        return n;
    }

    @Transactional
    public NotificationView read(Long id) {
        var n = mine(id);
        if (!n.getReadFlag()) {
            n.setReadFlag(true);
            n.setReadAt(clock.instant());
        }
        return view(n);
    }

    @Transactional
    public long readAll() {
        return em.createQuery("update Notification n set n.readFlag=true,n.readAt=:now where n.recipient.id=:uid and n.readFlag=false and n.deletedAt is null and (n.expiresAt is null or n.expiresAt>:now)").setParameter("now", clock.instant()).setParameter("uid", own().getId()).executeUpdate();
    }

    @Transactional
    public void dismiss(Long id) {
        mine(id).setDeletedAt(clock.instant());
    }

    @Transactional
    public Preferences preferences() {
        return preferenceView(lockedPreference());
    }

    private NotificationPreference lockedPreference() {
        User u = own();
        em.lock(u, LockModeType.PESSIMISTIC_WRITE);
        return preferences.findOne((r, q, cb) -> cb.equal(r.get("user").get("id"), u.getId())).orElseGet(() -> {
            var p = new NotificationPreference();
            p.setUser(u);
            p.setCreatedAt(clock.instant());
            p.setUpdatedAt(clock.instant());
            return preferences.saveAndFlush(p);
        });
    }

    @Transactional
    public Preferences updatePreferences(PreferenceRequest input) {
        var p = lockedPreference();
        p.setAnnouncementsEnabled(input.announcementsEnabled());
        p.setExamNotificationsEnabled(input.examNotificationsEnabled());
        p.setAssignmentNotificationsEnabled(input.assignmentNotificationsEnabled());
        p.setFeeNotificationsEnabled(input.feeNotificationsEnabled());
        p.setMaterialNotificationsEnabled(input.materialNotificationsEnabled());
        p.setAttendanceNotificationsEnabled(input.attendanceNotificationsEnabled());
        p.setUpdatedAt(clock.instant());
        return preferenceView(p);
    }

    private Preferences preferenceView(NotificationPreference p) {
        return new Preferences(p.getAnnouncementsEnabled(), p.getExamNotificationsEnabled(), p.getAssignmentNotificationsEnabled(), p.getFeeNotificationsEnabled(), p.getMaterialNotificationsEnabled(), p.getAttendanceNotificationsEnabled());
    }

    private NotificationView view(Notification n) {
        return new NotificationView(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getActionUrl(), n.getRelatedEntityType(), n.getRelatedEntityId(), n.getReadFlag(), n.getReadAt(), n.getCreatedAt(), n.getExpiresAt());
    }

    /**
     * Only server-generated, allowlisted internal routes may be persisted.
     */
    public static String safeAction(String url) {
        if (url == null) return null;
        if (url.length() > 300 || !url.matches("/(?:announcements|notifications|exams|assignments|my-assignments|my-results|my-fees|resources|my-resources|parent|dashboard)(?:/[A-Za-z0-9_-]+)*"))
            throw new IllegalArgumentException("Notification actions must be safe internal routes");
        return url;
    }
}
