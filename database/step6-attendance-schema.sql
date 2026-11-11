-- MySQL 8: additive schema; existing tables and records are retained.
-- Apply after Step 5 only when not relying on the existing development ddl-auto=update.
CREATE TABLE IF NOT EXISTS attendance_sessions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 batch_id BIGINT NOT NULL, session_date DATE NOT NULL, schedule_id BIGINT,
 start_time TIME(6), end_time TIME(6), occurrence_key VARCHAR(20) NOT NULL,
 topic VARCHAR(250), status ENUM('DRAFT','FINALIZED') NOT NULL,
 marked_by_id BIGINT NOT NULL, finalized_at DATETIME(6),
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT,
 UNIQUE KEY uk_attendance_occurrence(batch_id,session_date,occurrence_key),
 INDEX ix_attendance_session_date(session_date),
 INDEX ix_attendance_batch_date(batch_id,session_date),
 FOREIGN KEY(batch_id) REFERENCES batches(id),
 FOREIGN KEY(schedule_id) REFERENCES batch_schedules(id),
 FOREIGN KEY(marked_by_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS attendance_records (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 session_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 status ENUM('PRESENT','ABSENT','LATE','EXCUSED') NOT NULL, remarks VARCHAR(500),
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_attendance_session_student(session_id,student_id),
 INDEX ix_attendance_student_session(student_id,session_id),
 FOREIGN KEY(session_id) REFERENCES attendance_sessions(id),
 FOREIGN KEY(student_id) REFERENCES students(id)
);
