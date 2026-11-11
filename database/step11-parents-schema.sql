-- EduFlow Step 11: additive MySQL 8 schema.
-- Select the existing database. No existing tables or records are reset.
-- verified_at / changed_at are UTC instants; audit base timestamps are institute-local.
CREATE TABLE IF NOT EXISTS parent_profiles (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL UNIQUE,
 phone_number VARCHAR(16) NOT NULL,
 alternate_phone VARCHAR(16),
 contact_email VARCHAR(254),
 address VARCHAR(500),
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS parent_student_links (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 parent_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL,
 relationship_type VARCHAR(20) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 primary_guardian BOOLEAN NOT NULL DEFAULT FALSE,
 verified_by_id BIGINT,
 verified_at DATETIME(6),
 version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_parent_child(parent_id,student_id),
 INDEX ix_parent_link_access(parent_id,status,student_id),
 FOREIGN KEY(parent_id) REFERENCES parent_profiles(id),
 FOREIGN KEY(student_id) REFERENCES students(id),
 FOREIGN KEY(verified_by_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS parent_link_audits (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 link_id BIGINT NOT NULL,
 previous_status VARCHAR(20),
 new_status VARCHAR(20) NOT NULL,
 relationship_type VARCHAR(20) NOT NULL,
 primary_guardian BOOLEAN NOT NULL,
 changed_by_id BIGINT NOT NULL,
 changed_at DATETIME(6) NOT NULL,
 reason VARCHAR(1000) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY(link_id) REFERENCES parent_student_links(id),
 FOREIGN KEY(changed_by_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
