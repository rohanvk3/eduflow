package com.eduflow.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notifications.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler {
    final NotificationReminderService reminders;
    final NotificationDeliveryService delivery;
    final int batches;

    public NotificationScheduler(NotificationReminderService reminders, NotificationDeliveryService delivery, @Value("${app.notifications.delivery-batches-per-tick:10}") int batches) {
        if (batches < 1 || batches > 50) throw new IllegalArgumentException("Delivery batches per tick must be 1–50");
        this.reminders = reminders;
        this.delivery = delivery;
        this.batches = batches;
    }

    @Scheduled(initialDelayString = "${app.notifications.initial-delay-ms:60000}", fixedDelayString = "${app.notifications.poll-ms:60000}")
    public void tick() {
        try {
            reminders.scheduledAnnouncements();
            reminders.exams();
            reminders.assignments();
            reminders.fees();
            for (int i = 0; i < batches; i++) if (delivery.deliverBatch() == 0) break;
        } catch (RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Notification work will retry; failure type: {}", ex.getClass().getSimpleName());
        }
    }
}
