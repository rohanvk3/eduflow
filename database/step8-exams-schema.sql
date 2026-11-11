-- EduFlow Step 8, additive MySQL 8.0.16+ migration.
-- Select the existing database first. No existing tables or rows are changed.
-- Exam/attempt/publication instants are stored in UTC; eligibility_date uses Asia/Kolkata.
-- Cross-row question/mark/eligibility constraints are enforced by transactional services.

CREATE TABLE IF NOT EXISTS exams (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 exam_code VARCHAR(40) NOT NULL,
 title VARCHAR(200) NOT NULL,
 description VARCHAR(4000),
 batch_id BIGINT NOT NULL,
 exam_type VARCHAR(20) NOT NULL,
 duration_minutes INT,
 total_marks DECIMAL(10,2) NOT NULL,
 passing_marks DECIMAL(10,2) NOT NULL,
 start_date_time DATETIME(6) NOT NULL,
 end_date_time DATETIME(6) NOT NULL,
 eligibility_date DATE NOT NULL,
 status VARCHAR(20) NOT NULL,
 created_by_id BIGINT NOT NULL,
 results_published BOOLEAN NOT NULL DEFAULT FALSE,
 results_published_at DATETIME(6),
 results_published_by_id BIGINT,
 version BIGINT NOT NULL DEFAULT 0,
 cancellation_reason VARCHAR(1000),
 UNIQUE KEY uk_exam_code(exam_code),
 INDEX ix_exam_batch_time(batch_id,start_date_time),
 FOREIGN KEY (batch_id) REFERENCES batches(id),
 FOREIGN KEY (created_by_id) REFERENCES users(id),
 FOREIGN KEY (results_published_by_id) REFERENCES users(id),
 CHECK(total_marks>0 AND passing_marks>=0 AND passing_marks<=total_marks),
 CHECK(end_date_time>start_date_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_questions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 exam_id BIGINT NOT NULL,
 question_text VARCHAR(6000) NOT NULL,
 marks DECIMAL(10,2) NOT NULL,
 display_order INT NOT NULL,
 INDEX ix_exam_question_order(exam_id,display_order),
 FOREIGN KEY (exam_id) REFERENCES exams(id),
 CHECK(marks>0 AND display_order>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_options (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 question_id BIGINT NOT NULL,
 option_text VARCHAR(2000) NOT NULL,
 is_correct BOOLEAN NOT NULL,
 display_order INT NOT NULL,
 FOREIGN KEY (question_id) REFERENCES exam_questions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_attempts (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 exam_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 started_at DATETIME(6) NOT NULL,
 submitted_at DATETIME(6),
 expires_at DATETIME(6) NOT NULL,
 status VARCHAR(20) NOT NULL,
 obtained_marks DECIMAL(10,2),
 version BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_exam_attempt_student(exam_id,student_id),
 INDEX ix_attempt_expiry(status,expires_at),
 FOREIGN KEY (exam_id) REFERENCES exams(id),
 FOREIGN KEY (student_id) REFERENCES students(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_answers (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 attempt_id BIGINT NOT NULL,
 question_id BIGINT NOT NULL,
 selected_option_id BIGINT,
 awarded_marks DECIMAL(10,2),
 UNIQUE KEY uk_exam_answer_question(attempt_id,question_id),
 FOREIGN KEY (attempt_id) REFERENCES exam_attempts(id),
 FOREIGN KEY (question_id) REFERENCES exam_questions(id),
 FOREIGN KEY (selected_option_id) REFERENCES exam_options(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_results (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 exam_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 obtained_marks DECIMAL(10,2) NOT NULL,
 remarks VARCHAR(1000),
 evaluated_by_id BIGINT,
 evaluated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_exam_result_student(exam_id,student_id),
 FOREIGN KEY (exam_id) REFERENCES exams(id),
 FOREIGN KEY (student_id) REFERENCES students(id),
 FOREIGN KEY (evaluated_by_id) REFERENCES users(id),
 CHECK(obtained_marks>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_result_corrections (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 result_id BIGINT NOT NULL,
 previous_marks DECIMAL(10,2) NOT NULL,
 new_marks DECIMAL(10,2) NOT NULL,
 reason VARCHAR(1000) NOT NULL,
 corrected_by_id BIGINT NOT NULL,
 corrected_at DATETIME(6) NOT NULL,
 FOREIGN KEY (result_id) REFERENCES exam_results(id),
 FOREIGN KEY (corrected_by_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


