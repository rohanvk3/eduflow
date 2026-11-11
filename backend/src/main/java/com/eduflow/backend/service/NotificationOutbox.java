package com.eduflow.backend.service;

import com.eduflow.backend.entity.NotificationJob;
import com.eduflow.backend.entity.NotificationTarget;
import com.eduflow.backend.entity.NotificationType;
import com.eduflow.backend.repository.NotificationJobRepository;
import com.eduflow.backend.repository.NotificationTargetRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class NotificationOutbox {
    final NotificationJobRepository jobs;
    final NotificationTargetRepository targets;
    final NotificationAudienceService audience;
    final Clock clock;
    final AuditRecorder audit;

    public NotificationOutbox(NotificationJobRepository jobs, NotificationTargetRepository targets, NotificationAudienceService audience, @Qualifier("notificationClock") Clock clock,AuditRecorder audit) {
        this.jobs = jobs;
        this.targets = targets;
        this.audience = audience;
        this.clock = clock;
        this.audit=audit;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public NotificationJob capture(String eventKey, NotificationType type, String relatedType, Long relatedId, String context) {
        var previous = jobs.findOne((r, q, cb) -> cb.equal(r.get("eventKey"), eventKey));
        if (previous.isPresent()) return previous.get();
        if(type==NotificationType.PAYMENT_RECORDED||type==NotificationType.EXAM_RESULT_PUBLISHED||type==NotificationType.ASSIGNMENT_GRADED)
            audit.record(type.name(),relatedType,relatedId,"Verified business transition recorded");
        var j = new NotificationJob();
        j.setEventKey(eventKey);
        j.setType(type);
        j.setRelatedType(relatedType);
        j.setRelatedId(relatedId);
        j.setContextKey(context);
        j.setCreatedAt(clock.instant());
        jobs.saveAndFlush(j);
        long count = audience.snapshot(j, recipient -> {
            var t = new NotificationTarget();
            t.setJob(j);
            t.setUserId(recipient.userId());
            t.setStudentId(recipient.studentId());
            targets.save(t);
        });
        j.setRecipientCount(count);
        j.setComplete(count == 0);
        jobs.flush();
        return j;
    }
}
