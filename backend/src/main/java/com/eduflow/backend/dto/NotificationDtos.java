package com.eduflow.backend.dto;

import com.eduflow.backend.entity.AnnouncementAudience;
import com.eduflow.backend.entity.AnnouncementStatus;
import com.eduflow.backend.entity.NotificationType;
import com.eduflow.backend.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record NotificationView(Long id, NotificationType type, String title, String message, String actionUrl,
                                   String relatedEntityType, Long relatedEntityId, boolean isRead, Instant readAt,
                                   Instant createdAt, Instant expiresAt) {
    }

    public record PreferenceRequest(@NotNull Boolean announcementsEnabled, @NotNull Boolean examNotificationsEnabled,
                                    @NotNull Boolean assignmentNotificationsEnabled,
                                    @NotNull Boolean feeNotificationsEnabled,
                                    @NotNull Boolean materialNotificationsEnabled,
                                    @NotNull Boolean attendanceNotificationsEnabled) {
    }

    public record Preferences(boolean announcementsEnabled, boolean examNotificationsEnabled,
                              boolean assignmentNotificationsEnabled, boolean feeNotificationsEnabled,
                              boolean materialNotificationsEnabled, boolean attendanceNotificationsEnabled) {
    }

    public record AnnouncementRequest(@NotBlank @Size(max = 200) String title,
                                      @NotBlank @Size(max = 20000) String content,
                                      @NotNull AnnouncementAudience audienceType, Role targetRole, Long courseId,
                                      Long batchId, Instant publishAt, Long version) {
    }

    public record VersionRequest(@NotNull Long version) {
    }

    public record AnnouncementView(Long id, String title, String content, AnnouncementAudience audienceType,
                                   Role targetRole, Long courseId, String courseName, Long batchId, String batchName,
                                   String createdBy, AnnouncementStatus status, Instant publishAt, Instant publishedAt,
                                   Instant createdAt, long version, boolean canManage, long recipients, long delivered,
                                   long skipped, boolean deliveryComplete, String publicationIssue) {
    }

    public record DeliveryOverview(long jobs, long pendingJobs, long recipients, long delivered, long skipped,
                                   long scheduledAnnouncements) {
    }
}

