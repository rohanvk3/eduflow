package com.eduflow.backend.service;

import com.eduflow.backend.entity.Announcement;
import com.eduflow.backend.entity.AnnouncementStatus;
import com.eduflow.backend.entity.NotificationType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class AnnouncementPublication {
    final NotificationOutbox outbox;
    final NotificationAudienceService audience;
    final Clock clock;

    public AnnouncementPublication(NotificationOutbox outbox, NotificationAudienceService audience, @Qualifier("notificationClock") Clock clock) {
        this.outbox = outbox;
        this.audience = audience;
        this.clock = clock;
    }

    public boolean publish(Announcement a, boolean scheduled) {
        if (scheduled && !audience.authorAllowed(a)) {
            a.setPublicationIssue("Publication is blocked because the author no longer has permission. Ask an administrator to archive this notice and create an authorized replacement.");
            return false;
        }
        a.setPublicationIssue(null);
        a.setStatus(AnnouncementStatus.PUBLISHED);
        a.setPublishedAt(clock.instant());
        a.setUpdatedAt(clock.instant());
        var job = outbox.capture("announcement:" + a.getId(), NotificationType.ANNOUNCEMENT, "ANNOUNCEMENT", a.getId(), null);
        a.setDeliveryJobId(job.getId());
        return true;
    }
}
