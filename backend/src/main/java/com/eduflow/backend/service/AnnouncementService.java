package com.eduflow.backend.service;

import com.eduflow.backend.dto.NotificationDtos.AnnouncementRequest;
import com.eduflow.backend.dto.NotificationDtos.AnnouncementView;
import com.eduflow.backend.dto.NotificationDtos.DeliveryOverview;
import com.eduflow.backend.dto.NotificationDtos.VersionRequest;
import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.AcademicConflictException;
import com.eduflow.backend.repository.AnnouncementRepository;
import com.eduflow.backend.repository.NotificationJobRepository;
import com.eduflow.backend.repository.NotificationTargetRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@PreAuthorize("isAuthenticated()")
public class AnnouncementService {
    final AnnouncementRepository announcements;
    final NotificationJobRepository jobs;
    final NotificationTargetRepository targets;
    final AssignmentAccess a;
    final NotificationAudienceService audience;
    final AnnouncementPublication publication;
    final Clock clock;

    public AnnouncementService(AnnouncementRepository announcements, NotificationJobRepository jobs, NotificationTargetRepository targets, AssignmentAccess a, NotificationAudienceService audience, AnnouncementPublication publication, @Qualifier("notificationClock") Clock clock) {
        this.announcements = announcements;
        this.jobs = jobs;
        this.targets = targets;
        this.a = a;
        this.audience = audience;
        this.publication = publication;
        this.clock = clock;
    }

    boolean canManage(Announcement n) {
        return a.academic.hasRole("ADMIN") || (a.academic.hasRole("TEACHER") && n.getAudienceType() == AnnouncementAudience.BATCH && a.em.createQuery("select count(t) from BatchTeacherAssignment t where t.batch.id=:id and t.active=true and t.teacher.user.email=:email", Long.class).setParameter("id", n.getBatch().getId()).setParameter("email", a.academic.email()).getSingleResult() > 0);
    }

    void manage(Announcement n) {
        if (!canManage(n)) throw new AccessDeniedException("Announcement management denied");
    }

    Predicate manager(From<?, Announcement> r, CriteriaQuery<?> q, CriteriaBuilder cb) {
        if (a.academic.hasRole("ADMIN")) return cb.conjunction();
        if (!a.academic.hasRole("TEACHER")) return cb.disjunction();
        var sub = q.subquery(Long.class);
        var t = sub.from(BatchTeacherAssignment.class);
        sub.select(t.get("batch").get("id")).where(cb.isTrue(t.get("active")), cb.equal(t.get("teacher").get("user").get("email"), a.academic.email()));
        return cb.and(cb.equal(r.get("audienceType"), AnnouncementAudience.BATCH), r.get("batch").get("id").in(sub));
    }

    Predicate recipient(From<?, Announcement> r, CriteriaQuery<?> q, CriteriaBuilder cb, User u) {
        var sub = q.subquery(Long.class);
        var t = sub.from(NotificationTarget.class);
        sub.select(t.get("id")).where(cb.equal(t.get("job").get("id"), r.get("deliveryJobId")), cb.equal(t.get("userId"), u.getId()));
        return cb.and(cb.equal(r.get("status"), AnnouncementStatus.PUBLISHED), cb.exists(sub), audience.announcementScope(r, q, cb, u));
    }

    public PageResponse<AnnouncementView> list(String search, AnnouncementAudience type, AnnouncementStatus status, Long courseId, Long batchId, int page, int size, String sort, boolean mine) {
        return list(search, type, status, courseId, batchId, page, size, sort, mine, false);
    }

    public PageResponse<AnnouncementView> list(String search, AnnouncementAudience type, AnnouncementStatus status, Long courseId, Long batchId, int page, int size, String sort, boolean mine, boolean createdByMe) {
        CourseService.validatePage(page, size, search);
        String pattern = CourseService.pattern(search);
        User user = a.user();
        return PageResponse.from(announcements.findAll((r, q, cb) -> {
            var access = mine ? recipient(r, q, cb, user) : cb.or(manager(r, q, cb), recipient(r, q, cb, user));
            var p = cb.and(access, cb.like(cb.lower(r.get("title")), pattern, '\\'));
            if (createdByMe) p = cb.and(p, cb.equal(r.get("createdBy").get("id"), user.getId()));
            if (type != null) p = cb.and(p, cb.equal(r.get("audienceType"), type));
            if (status != null) p = cb.and(p, cb.equal(r.get("status"), status));
            if (batchId != null) p = cb.and(p, cb.equal(r.get("batch").get("id"), batchId));
            if (courseId != null) {
                var batches = q.subquery(Long.class);
                var b = batches.from(Batch.class);
                batches.select(b.get("id")).where(cb.equal(b.get("course").get("id"), courseId));
                p = cb.and(p, cb.or(cb.equal(r.get("course").get("id"), courseId), r.get("batch").get("id").in(batches)));
            }
            return p;
        }, FinanceService.paging(page, size, sort, Set.of("id", "title", "createdAt", "publishAt", "publishedAt", "status"))).map(this::view));
    }

    public AnnouncementView get(Long id) {
        var n = a.find(Announcement.class, id, "Announcement", false);
        if (!canManage(n)) {
            var u = a.user();
            if (n.getStatus() != AnnouncementStatus.PUBLISHED || n.getDeliveryJobId() == null || targets.count((r, q, cb) -> cb.and(cb.equal(r.get("job").get("id"), n.getDeliveryJobId()), cb.equal(r.get("userId"), u.getId()))) == 0 || !audience.member(n, u))
                throw new AccessDeniedException("Announcement access denied");
        }
        return view(n);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AnnouncementView save(Long id, AnnouncementRequest input) {
        var n = id == null ? new Announcement() : a.find(Announcement.class, id, "Announcement", true);
        if (id != null) {
            manage(n);
            a.version(n.getVersion(), input.version());
            if (n.getStatus() != AnnouncementStatus.DRAFT)
                throw new AcademicConflictException("Only draft announcements can be edited");
        }
        boolean valid = switch (input.audienceType()) {
            case ALL -> input.targetRole() == null && input.courseId() == null && input.batchId() == null;
            case ROLE -> input.targetRole() != null && input.courseId() == null && input.batchId() == null;
            case COURSE -> input.courseId() != null && input.targetRole() == null && input.batchId() == null;
            case BATCH -> input.batchId() != null && input.targetRole() == null && input.courseId() == null;
        };
        if (!valid) throw new IllegalArgumentException("Audience fields do not match the selected audience type");
        if (a.academic.hasRole("TEACHER")) {
            if (input.audienceType() != AnnouncementAudience.BATCH)
                throw new AccessDeniedException("Teachers may target assigned batches only");
            a.academic.readRoster(input.batchId());
        }
        if (input.publishAt() != null && !input.publishAt().isAfter(clock.instant()))
            throw new IllegalArgumentException("Scheduled publication must be in the future");
        n.setTitle(input.title().strip());
        n.setContent(input.content().strip());
        n.setAudienceType(input.audienceType());
        n.setTargetRole(input.targetRole());
        n.setCourse(input.courseId() == null ? null : a.find(Course.class, input.courseId(), "Course", false));
        n.setBatch(input.batchId() == null ? null : a.find(Batch.class, input.batchId(), "Batch", false));
        n.setPublishAt(input.publishAt());
        n.setUpdatedAt(clock.instant());
        if (id == null) {
            n.setCreatedBy(a.user());
            n.setCreatedAt(clock.instant());
        }
        announcements.saveAndFlush(n);
        return view(n);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AnnouncementView publish(Long id, VersionRequest input) {
        var n = a.find(Announcement.class, id, "Announcement", true);
        manage(n);
        a.version(n.getVersion(), input.version());
        if (n.getStatus() != AnnouncementStatus.DRAFT)
            throw new AcademicConflictException("Only draft announcements can be published or scheduled");
        if (n.getPublishAt() != null && n.getPublishAt().isAfter(clock.instant())) {
            n.setStatus(AnnouncementStatus.SCHEDULED);
            n.setUpdatedAt(clock.instant());
        } else publication.publish(n, false);
        announcements.flush();
        return view(n);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AnnouncementView archive(Long id, VersionRequest input) {
        var n = a.find(Announcement.class, id, "Announcement", true);
        manage(n);
        a.version(n.getVersion(), input.version());
        if (n.getStatus() == AnnouncementStatus.ARCHIVED)
            throw new AcademicConflictException("Announcement is already archived");
        n.setStatus(AnnouncementStatus.ARCHIVED);
        n.setUpdatedAt(clock.instant());
        announcements.flush();
        return view(n);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public DeliveryOverview overview() {
        boolean admin = a.academic.hasRole("ADMIN");
        org.springframework.data.jpa.domain.Specification<NotificationJob> scope = (r, q, cb) -> {
            if (admin) return cb.conjunction();
            var allowed = q.subquery(Long.class);
            var announcement = allowed.from(Announcement.class);
            allowed.select(announcement.get("id")).where(manager(announcement, q, cb));
            return cb.and(cb.equal(r.get("relatedType"), "ANNOUNCEMENT"), r.get("relatedId").in(allowed));
        };
        var cb = a.em.getCriteriaBuilder();
        var q = cb.createTupleQuery();
        var r = q.from(NotificationJob.class);
        q.multiselect(cb.count(r), cb.sum(r.<Long>get("recipientCount")), cb.sum(r.<Long>get("deliveredCount")), cb.sum(r.<Long>get("skippedCount"))).where(scope.toPredicate(r, q, cb));
        var row = a.em.createQuery(q).getSingleResult();
        return new DeliveryOverview(((Number) row.get(0)).longValue(), jobs.count(scope.and((x, c, b) -> b.isFalse(x.get("complete")))), number(row.get(1)), number(row.get(2)), number(row.get(3)), announcements.count((x, c, b) -> b.and(manager(x, c, b), b.equal(x.get("status"), AnnouncementStatus.SCHEDULED))));
    }

    long number(Object v) {
        return v == null ? 0 : ((Number) v).longValue();
    }

    AnnouncementView view(Announcement n) {
        boolean manage = canManage(n);
        var job = manage && n.getDeliveryJobId() != null ? jobs.findById(n.getDeliveryJobId()).orElse(null) : null;
        return new AnnouncementView(n.getId(), n.getTitle(), n.getContent(), n.getAudienceType(), n.getTargetRole(), n.getCourse() == null ? null : n.getCourse().getId(), n.getCourse() == null ? null : n.getCourse().getName(), n.getBatch() == null ? null : n.getBatch().getId(), n.getBatch() == null ? null : n.getBatch().getName(), n.getCreatedBy().getFullName(), n.getStatus(), n.getPublishAt(), n.getPublishedAt(), n.getCreatedAt(), n.getVersion(), manage, job == null ? 0 : job.getRecipientCount(), job == null ? 0 : job.getDeliveredCount(), job == null ? 0 : job.getSkippedCount(), job != null && job.getComplete(), manage ? n.getPublicationIssue() : null);
    }
}
