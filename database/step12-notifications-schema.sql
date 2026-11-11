-- EduFlow Step 12: additive MySQL 8 schema; select the EXISTING database first.
-- No existing tables, credentials, or business records are changed.
-- All timestamps in these six tables represent UTC instants.
CREATE TABLE IF NOT EXISTS notification_jobs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 event_key VARCHAR(180) NOT NULL,
 type VARCHAR(40) NOT NULL,
 related_type VARCHAR(40) NOT NULL,
 related_id BIGINT NOT NULL,
 context_key VARCHAR(120),
 created_at DATETIME(6) NOT NULL,
 complete BOOLEAN NOT NULL DEFAULT FALSE,
 recipient_count BIGINT NOT NULL DEFAULT 0,
 processed_count BIGINT NOT NULL DEFAULT 0,
 delivered_count BIGINT NOT NULL DEFAULT 0,
 skipped_count BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_notification_job_event(event_key),
 INDEX ix_notification_job_pending(complete,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS announcements (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 title VARCHAR(200) NOT NULL,
 content TEXT NOT NULL,
 audience_type VARCHAR(20) NOT NULL,
 target_role VARCHAR(20),
 course_id BIGINT,
 batch_id BIGINT,
 created_by_id BIGINT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 publish_at DATETIME(6),
 published_at DATETIME(6),
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 delivery_job_id BIGINT,
 publication_issue VARCHAR(250),
 version BIGINT NOT NULL DEFAULT 0,
 INDEX ix_announcement_schedule(status,publish_at),
 INDEX ix_announcement_scope(batch_id,status),
 FOREIGN KEY(course_id) REFERENCES courses(id),
 FOREIGN KEY(batch_id) REFERENCES batches(id),
 FOREIGN KEY(created_by_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_targets (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 job_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 student_id BIGINT,
 processed BOOLEAN NOT NULL DEFAULT FALSE,
 delivered BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_notification_target(job_id,user_id),
 INDEX ix_target_pending(job_id,processed,id),
 FOREIGN KEY(job_id) REFERENCES notification_jobs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Snapshot user_id/student_id are identifiers, not FKs. Missing recipients are
-- skipped safely at delivery; deleting a source must not destroy job history.

CREATE TABLE IF NOT EXISTS notifications (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 recipient_id BIGINT NOT NULL,
 type VARCHAR(40) NOT NULL,
 title VARCHAR(200) NOT NULL,
 message VARCHAR(1000) NOT NULL,
 action_url VARCHAR(300),
 related_entity_type VARCHAR(40),
 related_entity_id BIGINT,
 read_flag BOOLEAN NOT NULL DEFAULT FALSE,
 read_at DATETIME(6),
 created_at DATETIME(6) NOT NULL,
 expires_at DATETIME(6),
 deleted_at DATETIME(6),
 deduplication_key VARCHAR(180),
 UNIQUE KEY uk_notification_event(recipient_id,deduplication_key),
 INDEX ix_notification_inbox(recipient_id,deleted_at,created_at),
 INDEX ix_notification_unread(recipient_id,read_flag,deleted_at),
 FOREIGN KEY(recipient_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_preferences (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL UNIQUE,
 announcements_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 exam_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 assignment_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 fee_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 material_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 attendance_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_coordinators (
 name VARCHAR(40) NOT NULL PRIMARY KEY,
 last_id BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Operational coordination only; these are not demo/business records.
INSERT IGNORE INTO notification_coordinators(name,last_id) VALUES
 ('delivery',0),('reminders-exams',0),('reminders-assignments',0),
 ('reminders-fees',0),('scheduled-announcements',0);
