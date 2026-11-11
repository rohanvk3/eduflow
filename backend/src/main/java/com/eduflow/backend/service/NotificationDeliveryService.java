package com.eduflow.backend.service;

import com.eduflow.backend.config.NotificationSettings;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.NotificationJobRepository;
import com.eduflow.backend.repository.NotificationPreferenceRepository;
import com.eduflow.backend.repository.NotificationRepository;
import com.eduflow.backend.repository.NotificationTargetRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class NotificationDeliveryService {
    final NotificationJobRepository jobs;
    final NotificationTargetRepository targets;
    final NotificationRepository notifications;
    final NotificationPreferenceRepository preferences;
    final NotificationAudienceService audience;
    final NotificationCoordination coordination;
    final EntityManager em;
    final Clock clock;
    final NotificationSettings settings;

    public NotificationDeliveryService(NotificationJobRepository jobs, NotificationTargetRepository targets, NotificationRepository notifications, NotificationPreferenceRepository preferences, NotificationAudienceService audience, NotificationCoordination coordination, EntityManager em, @Qualifier("notificationClock") Clock clock, NotificationSettings settings) {
        this.jobs = jobs;
        this.targets = targets;
        this.notifications = notifications;
        this.preferences = preferences;
        this.audience = audience;
        this.coordination = coordination;
        this.em = em;
        this.clock = clock;
        this.settings = settings;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int deliverBatch() {
        coordination.lock("delivery");
        var pending = jobs.findAll((r, q, cb) -> cb.isFalse(r.get("complete")), PageRequest.of(0, 1, Sort.by("id")));
        if (pending.isEmpty()) return 0;
        var j = em.find(NotificationJob.class, pending.getContent().getFirst().getId(), LockModeType.PESSIMISTIC_WRITE);
        var rows = targets.findAll((r, q, cb) -> cb.and(cb.equal(r.get("job").get("id"), j.getId()), cb.isFalse(r.get("processed"))), PageRequest.of(0, settings.batchSize, Sort.by("id")));
        for (var t : rows) {
            var user = em.find(User.class, t.getUserId());
            var current = user == null ? null : audience.current(j, user.getId());
            boolean delivered = false;
            if (current != null && enabled(user, j.getType())) {
                var existing = notifications.findOne((r, q, cb) -> cb.and(cb.equal(r.get("recipient").get("id"), user.getId()), cb.equal(r.get("deduplicationKey"), j.getEventKey())));
                if (existing.isEmpty()) {
                    var n = new Notification();
                    n.setRecipient(user);
                    n.setType(j.getType());
                    n.setTitle(title(j.getType()));
                    n.setMessage(message(j.getType()));
                    n.setActionUrl(NotificationService.safeAction(action(j, user, current.studentId())));
                    n.setRelatedEntityType(j.getRelatedType());
                    n.setRelatedEntityId(j.getRelatedId());
                    n.setCreatedAt(clock.instant());
                    n.setDeduplicationKey(j.getEventKey());
                    notifications.saveAndFlush(n);
                }
                delivered = true;
            }
            t.setProcessed(true);
            t.setDelivered(delivered);
            j.setProcessedCount(j.getProcessedCount() + 1);
            if (delivered) j.setDeliveredCount(j.getDeliveredCount() + 1);
            else j.setSkippedCount(j.getSkippedCount() + 1);
        }
        j.setComplete(j.getProcessedCount() >= j.getRecipientCount());
        targets.flush();
        return rows.getNumberOfElements();
    }

    boolean enabled(User u, NotificationType type) {
        var p = preferences.findOne((r, q, cb) -> cb.equal(r.get("user").get("id"), u.getId())).orElse(null);
        return switch (type) {
            case GENERAL -> true;
            case ATTENDANCE_FINALIZED -> p != null && p.getAttendanceNotificationsEnabled();
            case ANNOUNCEMENT -> p == null || p.getAnnouncementsEnabled();
            case EXAM_SCHEDULED, EXAM_RESULT_PUBLISHED -> p == null || p.getExamNotificationsEnabled();
            case ASSIGNMENT_CREATED, ASSIGNMENT_DUE, ASSIGNMENT_GRADED ->
                    p == null || p.getAssignmentNotificationsEnabled();
            case FEE_DUE, FEE_OVERDUE, PAYMENT_RECORDED -> p == null || p.getFeeNotificationsEnabled();
            case STUDY_MATERIAL_PUBLISHED -> p == null || p.getMaterialNotificationsEnabled();
        };
    }

    private String action(NotificationJob j, User u, Long sid) {
        if (j.getType() == NotificationType.ANNOUNCEMENT) return "/announcements/" + j.getRelatedId();
        if (j.getType() == NotificationType.GENERAL) return "/notifications";
        if (u.getRole() == Role.PARENT) {
            String section = switch (j.getType()) {
                case EXAM_SCHEDULED -> "";
                case EXAM_RESULT_PUBLISHED -> "/results";
                case ASSIGNMENT_CREATED, ASSIGNMENT_DUE, ASSIGNMENT_GRADED -> "/assignments";
                case ATTENDANCE_FINALIZED -> "/attendance";
                default -> "/fees";
            };
            return "/parent/children/" + sid + section;
        }
        return switch (j.getType()) {
            case EXAM_SCHEDULED -> "/exams/" + j.getRelatedId();
            case EXAM_RESULT_PUBLISHED -> "/exams/" + j.getRelatedId() + "/result";
            case ASSIGNMENT_CREATED, ASSIGNMENT_DUE -> "/assignments/" + j.getRelatedId();
            case ASSIGNMENT_GRADED ->
                    "/assignments/" + em.find(AssignmentSubmission.class, j.getRelatedId()).getAssignment().getId() + "/submit";
            case STUDY_MATERIAL_PUBLISHED -> "/resources/materials/" + j.getRelatedId();
            case FEE_DUE, FEE_OVERDUE, PAYMENT_RECORDED -> "/my-fees";
            default -> "/notifications";
        };
    }

    private String title(NotificationType t) {
        return switch (t) {
            case ANNOUNCEMENT -> "New announcement";
            case EXAM_SCHEDULED -> "Exam scheduled";
            case EXAM_RESULT_PUBLISHED -> "Exam results published";
            case ASSIGNMENT_CREATED -> "New assignment";
            case ASSIGNMENT_DUE -> "Assignment deadline approaching";
            case ASSIGNMENT_GRADED -> "Assignment feedback available";
            case FEE_DUE -> "Upcoming fee installment";
            case FEE_OVERDUE -> "Fee installment needs attention";
            case PAYMENT_RECORDED -> "Payment recorded";
            case STUDY_MATERIAL_PUBLISHED -> "New learning resource";
            case ATTENDANCE_FINALIZED -> "Attendance updated";
            case GENERAL -> "Account update";
        };
    }

    private String message(NotificationType t) {
        return switch (t) {
            case ANNOUNCEMENT -> "An announcement is available. Open it to read the notice.";
            case EXAM_SCHEDULED -> "A published exam is available in your authorized exam view.";
            case EXAM_RESULT_PUBLISHED -> "Published results are available in your results view.";
            case ASSIGNMENT_CREATED -> "Published homework is available in your assignment view.";
            case ASSIGNMENT_DUE -> "An unsubmitted assignment is approaching its deadline.";
            case ASSIGNMENT_GRADED -> "Published marks and feedback are available for submitted work.";
            case FEE_DUE, FEE_OVERDUE -> "An installment has an outstanding balance. Open your fee view for details.";
            case PAYMENT_RECORDED -> "A verified institute payment entry is available in your fee history.";
            case STUDY_MATERIAL_PUBLISHED -> "A published learning resource is available in your library.";
            case ATTENDANCE_FINALIZED -> "Finalized attendance is available for a verified linked child.";
            case GENERAL -> "An account update is available.";
        };
    }
}
