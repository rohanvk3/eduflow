package com.eduflow.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationSettings {
    public final int assignmentHours, examHours, feeDays, batchSize;

    public NotificationSettings(@Value("${app.notifications.assignment-reminder-hours:24}") int assignmentHours, @Value("${app.notifications.exam-reminder-hours:24}") int examHours, @Value("${app.notifications.fee-reminder-days:3}") int feeDays, @Value("${app.notifications.batch-size:100}") int batchSize) {
        if (assignmentHours < 1 || assignmentHours > 720 || examHours < 1 || examHours > 720 || feeDays < 0 || feeDays > 30 || batchSize < 1 || batchSize > 500)
            throw new IllegalArgumentException("Invalid notification reminder/batch configuration");
        this.assignmentHours = assignmentHours;
        this.examHours = examHours;
        this.feeDays = feeDays;
        this.batchSize = batchSize;
    }
}

