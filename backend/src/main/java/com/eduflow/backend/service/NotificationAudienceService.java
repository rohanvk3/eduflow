package com.eduflow.backend.service;

import com.eduflow.backend.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Selects identifiers only. No recipient directory is exposed to clients.
 */
@Component
public class NotificationAudienceService {
    final EntityManager em;
    final Clock clock;

    public NotificationAudienceService(EntityManager em, @Qualifier("notificationClock") Clock clock) {
        this.em = em;
        this.clock = clock;
    }

    public record Recipient(Long userId, Long studentId) {
    }

    record Selection(String select, String where, String userPath, String group, Map<String, Object> args) {
    }

    private Selection students(String condition, Map<String, Object> args) {
        return new Selection("select distinct u.id,s.id", " from Student s join s.user u where u.role=com.eduflow.backend.entity.Role.STUDENT and " + condition, "u.id", "", args);
    }

    private Selection parents(String condition, Map<String, Object> args) {
        return new Selection("select l.parent.user.id,min(s.id)", " from ParentStudentLink l join l.student s where l.status=com.eduflow.backend.entity.ParentLinkStatus.VERIFIED and l.parent.status=com.eduflow.backend.entity.StudentStatus.ACTIVE and l.parent.user.role=com.eduflow.backend.entity.Role.PARENT and " + condition, "l.parent.user.id", " group by l.parent.user.id", args);
    }

    private String current(Long course, Long batch, Map<String, Object> args) {
        args.put("day", clock.instant().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate());
        String text = "exists(select n.id from BatchEnrollment n where n.student=s and n.status=com.eduflow.backend.entity.EnrollmentStatus.ACTIVE and n.enrollmentDate<=:day and (n.withdrawalDate is null or n.withdrawalDate>:day) and (n.completionDate is null or n.completionDate>=:day)";
        if (course != null) {
            args.put("course", course);
            text += " and n.batch.course.id=:course";
        }
        if (batch != null) {
            args.put("batch", batch);
            text += " and n.batch.id=:batch";
        }
        return text + ")";
    }

    public boolean authorAllowed(Announcement a) {
        if (a.getCreatedBy().getRole() == Role.ADMIN) return true;
        return a.getCreatedBy().getRole() == Role.TEACHER && a.getAudienceType() == AnnouncementAudience.BATCH && em.createQuery("select count(t) from BatchTeacherAssignment t where t.batch.id=:batch and t.active=true and t.teacher.user.id=:uid", Long.class).setParameter("batch", a.getBatch().getId()).setParameter("uid", a.getCreatedBy().getId()).getSingleResult() > 0;
    }

    private List<Selection> announcement(Announcement a) {
        var args = new HashMap<String, Object>();
        var rows = new ArrayList<Selection>();
        if (a.getAudienceType() == AnnouncementAudience.ALL || a.getAudienceType() == AnnouncementAudience.ROLE) {
            if (a.getAudienceType() != AnnouncementAudience.ROLE || a.getTargetRole() != Role.PARENT) {
                String filter = "u.role<>com.eduflow.backend.entity.Role.PARENT";
                if (a.getAudienceType() == AnnouncementAudience.ROLE) {
                    args.put("role", a.getTargetRole());
                    filter += " and u.role=:role";
                }
                rows.add(new Selection("select u.id,cast(null as long)", " from User u where " + filter, "u.id", "", args));
            }
            if (a.getAudienceType() == AnnouncementAudience.ALL || a.getTargetRole() == Role.PARENT)
                rows.add(parents("1=1", Map.of()));
            return rows;
        }
        Long course = a.getCourse() == null ? null : a.getCourse().getId(), batch = a.getBatch() == null ? null : a.getBatch().getId();
        String condition = current(course, batch, args);
        rows.add(students(condition, args));
        rows.add(parents(condition, args));
        var teacherArgs = new HashMap<String, Object>();
        String teacher = "t.active=true and t.teacher.user.role=com.eduflow.backend.entity.Role.TEACHER";
        if (course != null) {
            teacherArgs.put("course", course);
            teacher += " and t.batch.course.id=:course";
        }
        if (batch != null) {
            teacherArgs.put("batch", batch);
            teacher += " and t.batch.id=:batch";
        }
        rows.add(new Selection("select distinct t.teacher.user.id,cast(null as long)", " from BatchTeacherAssignment t where " + teacher, "t.teacher.user.id", "", teacherArgs));
        return rows;
    }

    List<Selection> selections(NotificationJob job) {
        var args = new HashMap<String, Object>();
        String condition;
        boolean students = true, parents = true;
        switch (job.getRelatedType()) {
            case "ANNOUNCEMENT" -> {
                var x = em.find(Announcement.class, job.getRelatedId());
                return x != null && x.getStatus() == AnnouncementStatus.PUBLISHED ? announcement(x) : List.of();
            }
            case "EXAM" -> {
                var e = em.find(Exam.class, job.getRelatedId());
                if (e == null) return List.of();
                if (job.getType() == NotificationType.EXAM_RESULT_PUBLISHED) {
                    if (!e.getResultsPublished() || e.getStatus() == ExamStatus.CANCELLED) return List.of();
                } else if (e.getStatus() != ExamStatus.PUBLISHED || !e.getEndDateTime().isAfter(clock.instant()))
                    return List.of();
                if (job.getContextKey() != null && !job.getContextKey().equals(e.getStartDateTime().toString()))
                    return List.of();
                args.put("exam", e.getId());
                args.put("batch", e.getBatch().getId());
                args.put("date", e.getEligibilityDate());
                condition = "exists(select n.id from BatchEnrollment n where n.student=s and n.batch.id=:batch and n.enrollmentDate<=:date and (n.withdrawalDate is null or n.withdrawalDate>:date) and (n.completionDate is null or n.completionDate>=:date))";
                if (job.getType() == NotificationType.EXAM_RESULT_PUBLISHED)
                    condition += " and exists(select r.id from ExamResult r where r.exam.id=:exam and r.student=s)";
                else args.remove("exam");
            }
            case "ASSIGNMENT" -> {
                var x = em.find(Assignment.class, job.getRelatedId());
                if (x == null || x.getStatus() != AssignmentStatus.PUBLISHED) return List.of();
                args.put("assignment", x.getId());
                condition = "exists(select r.id from AssignmentRecipient r where r.assignment.id=:assignment and r.student=s)";
                if (job.getType() == NotificationType.ASSIGNMENT_DUE) {
                    if (!x.getDueDateTime().isAfter(clock.instant())) return List.of();
                    condition += " and not exists(select sub.id from AssignmentSubmission sub where sub.assignment.id=:assignment and sub.student=s) and " + current(null, x.getBatch().getId(), args);
                }
            }
            case "SUBMISSION" -> {
                var x = em.find(AssignmentSubmission.class, job.getRelatedId());
                if (x == null || x.getStatus() != SubmissionStatus.GRADED) return List.of();
                args.put("sid", x.getStudent().getId());
                condition = "s.id=:sid";
            }
            case "PAYMENT" -> {
                var x = em.find(FeePayment.class, job.getRelatedId());
                if (x == null || x.getStatus() != FeePaymentStatus.SUCCESS) return List.of();
                args.put("sid", x.getFeeAccount().getStudent().getId());
                condition = "s.id=:sid";
            }
            case "INSTALLMENT" -> {
                var x = em.find(FeeInstallment.class, job.getRelatedId());
                if (x == null || x.getFeeAccount().getStatus() != FeeAccountStatus.ACTIVE || !x.getDueDate().toString().equals(job.getContextKey()))
                    return List.of();
                var paid = em.createQuery("select coalesce(sum(a.amount),0) from PaymentAllocation a where a.installment.id=:id and a.payment.status=com.eduflow.backend.entity.FeePaymentStatus.SUCCESS", java.math.BigDecimal.class).setParameter("id", x.getId()).getSingleResult();
                if (x.getAmount().compareTo(paid) <= 0) return List.of();
                LocalDate day = clock.instant().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
                if (job.getType() == NotificationType.FEE_OVERDUE ? !x.getDueDate().isBefore(day) : x.getDueDate().isBefore(day))
                    return List.of();
                if (job.getType() == NotificationType.FEE_OVERDUE && !job.getEventKey().endsWith(":" + day.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))))
                    return List.of();
                args.put("sid", x.getFeeAccount().getStudent().getId());
                condition = "s.id=:sid";
            }
            case "MATERIAL" -> {
                var x = em.find(StudyMaterial.class, job.getRelatedId());
                if (x == null || x.getVisibility() != ResourceVisibility.PUBLISHED || !Objects.equals(job.getContextKey(), String.valueOf(x.getPublishedVersion())))
                    return List.of();
                condition = current(x.getCourse().getId(), x.getBatch() == null ? null : x.getBatch().getId(), args);
                parents = false;
            }
            case "ATTENDANCE" -> {
                var x = em.find(AttendanceSession.class, job.getRelatedId());
                if (x == null || x.getStatus() != AttendanceSessionStatus.FINALIZED) return List.of();
                args.put("session", x.getId());
                condition = "exists(select r.id from AttendanceRecord r where r.session.id=:session and r.student=s)";
                students = false;
            }
            case "USER" -> {
                return List.of(new Selection("select u.id,cast(null as long)", " from User u where u.id=:id", "u.id", "", Map.of("id", job.getRelatedId())));
            }
            default -> {
                return List.of();
            }
        }
        var selected = new ArrayList<Selection>();
        if (students) selected.add(students(condition, args));
        if (parents) selected.add(parents(condition, args));
        return selected;
    }

    private List<Recipient> query(Selection s, Long userId, int offset, int size) {
        var q = em.createQuery(s.select() + s.where() + (userId == null ? "" : " and " + s.userPath() + "=:recipient") + s.group() + " order by " + s.userPath(), Object[].class);
        s.args().forEach(q::setParameter);
        if (userId != null) q.setParameter("recipient", userId);
        return q.setFirstResult(offset).setMaxResults(size).getResultList().stream().map(r -> new Recipient(((Number) r[0]).longValue(), r[1] == null ? null : ((Number) r[1]).longValue())).toList();
    }

    public long snapshot(NotificationJob job, java.util.function.Consumer<Recipient> consumer) {
        long total = 0;
        for (var s : selections(job)) {
            int offset = 0;
            while (true) {
                var rows = query(s, null, offset, 500);
                for (var r : rows) {
                    consumer.accept(r);
                    total++;
                }
                if (rows.size() < 500) break;
                offset += rows.size();
            }
        }
        return total;
    }

    public Recipient current(NotificationJob job, Long uid) {
        for (var s : selections(job)) {
            var r = query(s, uid, 0, 1);
            if (!r.isEmpty()) return r.getFirst();
        }
        return null;
    }

    public boolean member(Announcement a, User u) {
        for (var s : announcement(a)) if (!query(s, u.getId(), 0, 1).isEmpty()) return true;
        return false;
    }

    public Predicate announcementScope(From<?, Announcement> r, CriteriaQuery<?> q, CriteriaBuilder cb, User u) {
        var general = cb.or(cb.equal(r.get("audienceType"), AnnouncementAudience.ALL), cb.and(cb.equal(r.get("audienceType"), AnnouncementAudience.ROLE), cb.equal(r.get("targetRole"), u.getRole())));
        var day = clock.instant().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
        if (u.getRole() == Role.PARENT) {
            var link = q.subquery(Long.class);
            var l = link.from(ParentStudentLink.class);
            var enrollment = link.subquery(Long.class);
            var n = enrollment.from(BatchEnrollment.class);
            enrollment.select(n.get("id")).where(cb.equal(n.get("student").get("id"), l.get("student").get("id")), active(n, cb, day), target(r, n.get("batch"), cb));
            link.select(l.get("id")).where(cb.equal(l.get("parent").get("user").get("id"), u.getId()), cb.equal(l.get("parent").get("status"), StudentStatus.ACTIVE), cb.equal(l.get("status"), ParentLinkStatus.VERIFIED), cb.or(general, cb.exists(enrollment)));
            return cb.exists(link);
        }
        if (u.getRole() == Role.STUDENT) {
            var sub = q.subquery(Long.class);
            var n = sub.from(BatchEnrollment.class);
            sub.select(n.get("id")).where(cb.equal(n.get("student").get("user").get("id"), u.getId()), active(n, cb, day), target(r, n.get("batch"), cb));
            return cb.or(general, cb.exists(sub));
        }
        if (u.getRole() == Role.TEACHER) {
            var sub = q.subquery(Long.class);
            var t = sub.from(BatchTeacherAssignment.class);
            sub.select(t.get("id")).where(cb.equal(t.get("teacher").get("user").get("id"), u.getId()), cb.isTrue(t.get("active")), target(r, t.get("batch"), cb));
            return cb.or(general, cb.exists(sub));
        }
        return general;
    }

    private Predicate active(From<?, BatchEnrollment> n, CriteriaBuilder cb, LocalDate day) {
        return cb.and(cb.equal(n.get("status"), EnrollmentStatus.ACTIVE), cb.lessThanOrEqualTo(n.get("enrollmentDate"), day), cb.or(cb.isNull(n.get("withdrawalDate")), cb.greaterThan(n.get("withdrawalDate"), day)), cb.or(cb.isNull(n.get("completionDate")), cb.greaterThanOrEqualTo(n.get("completionDate"), day)));
    }

    private Predicate target(From<?, Announcement> r, Path<?> batch, CriteriaBuilder cb) {
        return cb.or(cb.and(cb.equal(r.get("audienceType"), AnnouncementAudience.COURSE), cb.equal(r.get("course").get("id"), batch.get("course").get("id"))), cb.and(cb.equal(r.get("audienceType"), AnnouncementAudience.BATCH), cb.equal(r.get("batch").get("id"), batch.get("id"))));
    }
}
