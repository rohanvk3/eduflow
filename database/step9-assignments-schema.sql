-- EduFlow Step 9: additive MySQL 8.0.16+ schema.
-- Select the existing database first. No existing tables or rows are changed.
-- Deadlines/submission/grading instants use UTC; created_at/updated_at are institute-local.
-- Cross-row eligibility, marks limits and file workflow are enforced transactionally by services.

CREATE TABLE IF NOT EXISTS assignments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 title VARCHAR(200) NOT NULL,
 description TEXT,
 batch_id BIGINT NOT NULL,
 created_by_id BIGINT NOT NULL,
 max_marks DECIMAL(10,2) NOT NULL,
 published_at DATETIME(6),
 due_date_time DATETIME(6) NOT NULL,
 allow_late_submission BOOLEAN NOT NULL DEFAULT FALSE,
 status VARCHAR(20) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 INDEX ix_assignment_batch_due(batch_id,due_date_time),
 FOREIGN KEY (batch_id) REFERENCES batches(id),
 FOREIGN KEY (created_by_id) REFERENCES users(id),
 CHECK(max_marks>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_recipients (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 assignment_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 UNIQUE KEY uk_assignment_recipient(assignment_id,student_id),
 FOREIGN KEY (assignment_id) REFERENCES assignments(id),
 FOREIGN KEY (student_id) REFERENCES students(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_attachments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 assignment_id BIGINT NOT NULL,
 original_file_name VARCHAR(180) NOT NULL,
 storage_key VARCHAR(60) NOT NULL,
 content_type VARCHAR(120) NOT NULL,
 file_size BIGINT NOT NULL,
 uploaded_by_id BIGINT NOT NULL,
 FOREIGN KEY (assignment_id) REFERENCES assignments(id),
 FOREIGN KEY (uploaded_by_id) REFERENCES users(id),
 CHECK(file_size>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignment_submissions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 assignment_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 submission_text MEDIUMTEXT,
 submitted_at DATETIME(6) NOT NULL,
 status VARCHAR(20) NOT NULL,
 late BOOLEAN NOT NULL DEFAULT FALSE,
 obtained_marks DECIMAL(10,2),
 feedback VARCHAR(5000),
 graded_by_id BIGINT,
 graded_at DATETIME(6),
 version BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_assignment_submission(assignment_id,student_id),
 INDEX ix_submission_status(status,submitted_at),
 FOREIGN KEY (assignment_id) REFERENCES assignments(id),
 FOREIGN KEY (student_id) REFERENCES students(id),
 FOREIGN KEY (graded_by_id) REFERENCES users(id),
 CHECK(obtained_marks IS NULL OR obtained_marks>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS submission_attachments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 submission_id BIGINT NOT NULL,
 original_file_name VARCHAR(180) NOT NULL,
 storage_key VARCHAR(60) NOT NULL,
 content_type VARCHAR(120) NOT NULL,
 file_size BIGINT NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 FOREIGN KEY (submission_id) REFERENCES assignment_submissions(id),
 CHECK(file_size>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS submission_revisions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 submission_id BIGINT NOT NULL,
 revision_number BIGINT NOT NULL,
 submission_text MEDIUMTEXT,
 submitted_at DATETIME(6) NOT NULL,
 status VARCHAR(20) NOT NULL,
 attachment_ids VARCHAR(2000) NOT NULL,
 changed_by_id BIGINT NOT NULL,
 changed_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_submission_revision(submission_id,revision_number),
 FOREIGN KEY (submission_id) REFERENCES assignment_submissions(id),
 FOREIGN KEY (changed_by_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS submission_grade_history (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 submission_id BIGINT NOT NULL,
 previous_marks DECIMAL(10,2),
 new_marks DECIMAL(10,2) NOT NULL,
 previous_feedback VARCHAR(5000),
 new_feedback VARCHAR(5000),
 reason VARCHAR(1000) NOT NULL,
 graded_by_id BIGINT NOT NULL,
 graded_at DATETIME(6) NOT NULL,
 FOREIGN KEY (submission_id) REFERENCES assignment_submissions(id),
 FOREIGN KEY (graded_by_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


