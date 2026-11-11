-- Additive MySQL 8 schema for environments that do not use Hibernate ddl-auto=update.
-- Run against the existing eduflow database. Never drops or resets existing tables.
-- CREATE IF NOT EXISTS leaves already-created tables untouched.
CREATE TABLE IF NOT EXISTS courses (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(40) NOT NULL UNIQUE, name VARCHAR(150) NOT NULL,
 description VARCHAR(2000), duration_weeks INT NOT NULL,
 status ENUM('ACTIVE','INACTIVE') NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL
);
CREATE TABLE IF NOT EXISTS batches (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(40) NOT NULL UNIQUE, name VARCHAR(150) NOT NULL,
 course_id BIGINT NOT NULL, capacity INT NOT NULL,
 start_date DATE NOT NULL, end_date DATE NOT NULL,
 status ENUM('PLANNED','ACTIVE','COMPLETED','CANCELLED') NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY (course_id) REFERENCES courses(id)
);
CREATE TABLE IF NOT EXISTS batch_teacher_assignments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 batch_id BIGINT NOT NULL, teacher_id BIGINT NOT NULL, active BIT(1) NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_assignment(batch_id,teacher_id),
 FOREIGN KEY(batch_id) REFERENCES batches(id), FOREIGN KEY(teacher_id) REFERENCES teachers(id)
);
CREATE TABLE IF NOT EXISTS batch_enrollments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 batch_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 enrollment_date DATE NOT NULL, withdrawal_date DATE, completion_date DATE,
 status ENUM('ACTIVE','WITHDRAWN','COMPLETED') NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_enrollment_period(batch_id,student_id,enrollment_date),
 INDEX ix_enrollment_eligibility(batch_id,student_id,enrollment_date,withdrawal_date),
 FOREIGN KEY(batch_id) REFERENCES batches(id), FOREIGN KEY(student_id) REFERENCES students(id)
);
CREATE TABLE IF NOT EXISTS batch_schedules (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 batch_id BIGINT NOT NULL, teacher_id BIGINT NOT NULL,
 day_of_week ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
 start_time TIME(6) NOT NULL, end_time TIME(6) NOT NULL, room VARCHAR(100), active BIT(1) NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 INDEX ix_schedule_teacher_day(teacher_id,day_of_week,active),
 FOREIGN KEY(batch_id) REFERENCES batches(id), FOREIGN KEY(teacher_id) REFERENCES teachers(id)
);
