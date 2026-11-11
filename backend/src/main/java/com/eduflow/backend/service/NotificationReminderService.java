package com.eduflow.backend.service;

import com.eduflow.backend.config.NotificationSettings;
import com.eduflow.backend.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

@Service
public class NotificationReminderService {
    final EntityManager em;
    final NotificationCoordination coordination;
    final NotificationOutbox outbox;
    final AnnouncementPublication publication;
    final Clock clock;
    final NotificationSettings settings;

    public NotificationReminderService(EntityManager em, NotificationCoordination coordination, NotificationOutbox outbox, AnnouncementPublication publication, @Qualifier("notificationClock") Clock clock, NotificationSettings settings) {
        this.em = em;
        this.coordination = coordination;
        this.outbox = outbox;
        this.publication = publication;
        this.clock = clock;
        this.settings = settings;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int scheduledAnnouncements() {
        var state = coordination.lock("scheduled-announcements");
        var rows = em.createQuery("select a from Announcement a where a.id>:cursor and a.status=com.eduflow.backend.entity.AnnouncementStatus.SCHEDULED and a.publishAt<=:now order by a.id", Announcement.class).setParameter("cursor", state.getLastId()).setParameter("now", clock.instant()).setMaxResults(settings.batchSize).getResultList();
        int done = 0;
        for (var a : rows) {
            em.refresh(a, LockModeType.PESSIMISTIC_WRITE);
            if (a.getStatus() == AnnouncementStatus.SCHEDULED && publication.publish(a, true)) done++;
        }
        state.setLastId(rows.size() < settings.batchSize ? 0 : rows.getLast().getId());
        return done;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int exams() {
        var state = coordination.lock("reminders-exams");
        Instant now = clock.instant();
        var rows = em.createQuery("select e from Exam e where e.id>:cursor and e.status=com.eduflow.backend.entity.ExamStatus.PUBLISHED and e.startDateTime>:now and e.startDateTime<=:limit order by e.id", Exam.class).setParameter("cursor", state.getLastId()).setParameter("now", now).setParameter("limit", now.plus(Duration.ofHours(settings.examHours))).setMaxResults(settings.batchSize).getResultList();
        for (var e : rows)
            outbox.capture("exam-reminder:" + e.getId() + ":" + e.getStartDateTime(), NotificationType.EXAM_SCHEDULED, "EXAM", e.getId(), e.getStartDateTime().toString());
        state.setLastId(rows.size() < settings.batchSize ? 0 : rows.getLast().getId());
        return rows.size();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int assignments() {
        var state = coordination.lock("reminders-assignments");
        Instant now = clock.instant();
        var rows = em.createQuery("select a from Assignment a where a.id>:cursor and a.status=com.eduflow.backend.entity.AssignmentStatus.PUBLISHED and a.dueDateTime>:now and a.dueDateTime<=:limit order by a.id", Assignment.class).setParameter("cursor", state.getLastId()).setParameter("now", now).setParameter("limit", now.plus(Duration.ofHours(settings.assignmentHours))).setMaxResults(settings.batchSize).getResultList();
        for (var a : rows)
            outbox.capture("assignment-due:" + a.getId() + ":" + a.getDueDateTime(), NotificationType.ASSIGNMENT_DUE, "ASSIGNMENT", a.getId(), null);
        state.setLastId(rows.size() < settings.batchSize ? 0 : rows.getLast().getId());
        return rows.size();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int fees() {
        var state = coordination.lock("reminders-fees");
        LocalDate today = clock.instant().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
        var rows = em.createQuery("select i from FeeInstallment i where i.id>:cursor and i.feeAccount.status=com.eduflow.backend.entity.FeeAccountStatus.ACTIVE and i.dueDate<=:limit and i.amount>(select coalesce(sum(a.amount),0) from PaymentAllocation a where a.installment=i and a.payment.status=com.eduflow.backend.entity.FeePaymentStatus.SUCCESS) order by i.id", FeeInstallment.class).setParameter("cursor", state.getLastId()).setParameter("limit", today.plusDays(settings.feeDays)).setMaxResults(settings.batchSize).getResultList();
        String week = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
        for (var i : rows) {
            boolean overdue = i.getDueDate().isBefore(today);
            outbox.capture((overdue ? "fee-overdue:" : "fee-due:") + i.getId() + ":" + i.getDueDate() + (overdue ? ":" + week : ""), overdue ? NotificationType.FEE_OVERDUE : NotificationType.FEE_DUE, "INSTALLMENT", i.getId(), i.getDueDate().toString());
        }
        state.setLastId(rows.size() < settings.batchSize ? 0 : rows.getLast().getId());
        return rows.size();
    }
}
