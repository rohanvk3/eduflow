-- Select the EXISTING EduFlow MySQL 8 database first. Additive and rerunnable.
-- No existing records, credentials, roles or passwords are changed.
SET @eduflow_ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='users' AND column_name='status'),'SELECT 1','ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''ACTIVE''');
PREPARE eduflow_change FROM @eduflow_ddl; EXECUTE eduflow_change; DEALLOCATE PREPARE eduflow_change;
SET @eduflow_ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='users' AND column_name='security_version'),'SELECT 1','ALTER TABLE users ADD COLUMN security_version BIGINT NOT NULL DEFAULT 0');
PREPARE eduflow_change FROM @eduflow_ddl; EXECUTE eduflow_change; DEALLOCATE PREPARE eduflow_change;
SET @eduflow_ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='users' AND column_name='reset_requested_at'),'SELECT 1','ALTER TABLE users ADD COLUMN reset_requested_at DATETIME(6) NULL');
PREPARE eduflow_change FROM @eduflow_ddl; EXECUTE eduflow_change; DEALLOCATE PREPARE eduflow_change;

CREATE TABLE IF NOT EXISTS institute_settings (
 id BIGINT NOT NULL PRIMARY KEY,
 institute_name VARCHAR(150) NOT NULL,
 institute_code VARCHAR(30) NOT NULL,
 email VARCHAR(254),phone_number VARCHAR(16),alternate_phone VARCHAR(16),
 address_line1 VARCHAR(250),address_line2 VARCHAR(250),city VARCHAR(100),
 state VARCHAR(100),postal_code VARCHAR(20),country VARCHAR(100),website VARCHAR(300),
 timezone VARCHAR(80) NOT NULL,currency_code VARCHAR(3) NOT NULL,
 academic_year_start DATE,academic_year_end DATE,
 logo_storage_key VARCHAR(80),logo_content_type VARCHAR(40),
 created_at DATETIME(6),updated_at DATETIME(6),version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- The application initializes id=1 only when absent. It does not replace settings.

CREATE TABLE IF NOT EXISTS audit_logs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 actor_user_id BIGINT,actor_role VARCHAR(20),action VARCHAR(80) NOT NULL,
 entity_type VARCHAR(60) NOT NULL,entity_id BIGINT,description VARCHAR(500) NOT NULL,
 outcome VARCHAR(20) NOT NULL,event_time DATETIME(6) NOT NULL,request_id VARCHAR(36),
 INDEX ix_audit_time(event_time,id),INDEX ix_audit_actor(actor_user_id,event_time),
 INDEX ix_audit_action(action,event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Actor IDs intentionally have no destructive cascade or FK: historical audit
-- records must survive later account lifecycle changes. No edit/delete APIs exist.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
 token_hash VARCHAR(64) NOT NULL PRIMARY KEY,
 user_id BIGINT NOT NULL,expires_at DATETIME(6) NOT NULL,used_at DATETIME(6),
 security_version BIGINT NOT NULL,
 INDEX ix_reset_user(user_id,expires_at),FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS recovery_throttles (
 client_hash VARCHAR(64) NOT NULL PRIMARY KEY,next_allowed_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
