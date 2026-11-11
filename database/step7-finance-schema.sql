-- EduFlow Step 7: additive MySQL 8.0.16+ schema. Select the existing database first.
-- Creates only new tables. No existing module tables or rows are modified.
-- Application locks and transactions enforce cross-row financial invariants.

CREATE TABLE IF NOT EXISTS fee_structures (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 course_id BIGINT NOT NULL,
 name VARCHAR(150) NOT NULL,
 tuition_fee DECIMAL(14,2) NOT NULL,
 admission_fee DECIMAL(14,2) NOT NULL,
 material_fee DECIMAL(14,2) NOT NULL,
 other_fee DECIMAL(14,2) NOT NULL,
 total_fee DECIMAL(14,2) NOT NULL,
 effective_from DATE NOT NULL,
 effective_to DATE,
 status VARCHAR(20) NOT NULL,
 FOREIGN KEY (course_id) REFERENCES courses(id),
 INDEX ix_fee_structure_course_dates(course_id,effective_from,effective_to),
 CHECK (tuition_fee>=0 AND admission_fee>=0 AND material_fee>=0 AND other_fee>=0),
 CHECK (total_fee=tuition_fee+admission_fee+material_fee+other_fee)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS student_fee_accounts (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 student_id BIGINT NOT NULL,
 enrollment_id BIGINT NOT NULL,
 fee_structure_id BIGINT,
 structure_name VARCHAR(150) NOT NULL,
 agreement_date DATE NOT NULL,
 tuition_fee DECIMAL(14,2) NOT NULL,
 admission_fee DECIMAL(14,2) NOT NULL,
 material_fee DECIMAL(14,2) NOT NULL,
 other_fee DECIMAL(14,2) NOT NULL,
 original_amount DECIMAL(14,2) NOT NULL,
 discount_amount DECIMAL(14,2) NOT NULL,
 final_payable_amount DECIMAL(14,2) NOT NULL,
 status VARCHAR(20) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_fee_account_enrollment(enrollment_id),
 INDEX ix_fee_account_student(student_id),
 FOREIGN KEY (student_id) REFERENCES students(id),
 FOREIGN KEY (enrollment_id) REFERENCES batch_enrollments(id),
 FOREIGN KEY (fee_structure_id) REFERENCES fee_structures(id),
 CHECK (discount_amount>=0 AND original_amount>=discount_amount AND final_payable_amount=original_amount-discount_amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fee_installments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 fee_account_id BIGINT NOT NULL,
 installment_number INT NOT NULL,
 amount DECIMAL(14,2) NOT NULL,
 due_date DATE NOT NULL,
 UNIQUE KEY uk_fee_installment_number(fee_account_id,installment_number),
 INDEX ix_fee_installment_due(due_date),
 FOREIGN KEY (fee_account_id) REFERENCES student_fee_accounts(id),
 CHECK (amount>0 AND installment_number>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fee_payments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 fee_account_id BIGINT NOT NULL,
 amount DECIMAL(14,2) NOT NULL,
 payment_date DATE NOT NULL,
 payment_method VARCHAR(20) NOT NULL,
 transaction_reference VARCHAR(150),
 external_reference_key VARCHAR(180),
 receipt_number VARCHAR(60) NOT NULL,
 idempotency_key VARCHAR(100) NOT NULL,
 request_hash VARCHAR(64) NOT NULL,
 status VARCHAR(20) NOT NULL,
 recorded_by_id BIGINT NOT NULL,
 remarks VARCHAR(1000),
 UNIQUE KEY uk_fee_receipt(receipt_number),
 UNIQUE KEY uk_fee_idempotency(idempotency_key),
 UNIQUE KEY uk_fee_external_reference(external_reference_key),
 INDEX ix_fee_payment_date_status(payment_date,status),
 FOREIGN KEY (fee_account_id) REFERENCES student_fee_accounts(id),
 FOREIGN KEY (recorded_by_id) REFERENCES users(id),
 CHECK (amount>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_allocations (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 payment_id BIGINT NOT NULL,
 installment_id BIGINT NOT NULL,
 amount DECIMAL(14,2) NOT NULL,
 UNIQUE KEY uk_payment_installment(payment_id,installment_id),
 FOREIGN KEY (payment_id) REFERENCES fee_payments(id),
 FOREIGN KEY (installment_id) REFERENCES fee_installments(id),
 CHECK (amount>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_void_audits (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 payment_id BIGINT NOT NULL,
 changed_by_id BIGINT NOT NULL,
 reason VARCHAR(1000) NOT NULL,
 previous_status VARCHAR(20) NOT NULL,
 new_status VARCHAR(20) NOT NULL,
 previous_amount DECIMAL(14,2) NOT NULL,
 UNIQUE KEY uk_payment_void(payment_id),
 FOREIGN KEY (payment_id) REFERENCES fee_payments(id),
 FOREIGN KEY (changed_by_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


