-- EduFlow Step 10: additive MySQL 8 schema.
-- Select the existing database, then execute this script before deployment when
-- automatic Hibernate schema updates are disabled. Existing tables/data are untouched.
-- All files reuse Step 9's private store. No public file URLs are created.
CREATE TABLE IF NOT EXISTS resource_categories (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 normalized_name VARCHAR(100) NOT NULL UNIQUE,
 description VARCHAR(2000),
 icon VARCHAR(40),
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_folders (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(150) NOT NULL,
 description VARCHAR(2000),
 course_id BIGINT NOT NULL,
 batch_id BIGINT,
 parent_folder_id BIGINT,
 created_by_id BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY(course_id) REFERENCES courses(id),
 FOREIGN KEY(batch_id) REFERENCES batches(id),
 FOREIGN KEY(parent_folder_id) REFERENCES resource_folders(id),
 FOREIGN KEY(created_by_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS study_materials (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 title VARCHAR(180) NOT NULL,
 description TEXT,
 course_id BIGINT NOT NULL,
 batch_id BIGINT,
 category_id BIGINT NOT NULL,
 folder_id BIGINT,
 uploaded_by_id BIGINT NOT NULL,
 resource_type VARCHAR(20) NOT NULL,
 visibility VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 external_url VARCHAR(2000),
 current_version INT NOT NULL DEFAULT 0,
 published_version INT,
 published_at DATETIME(6),
 version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 INDEX ix_material_scope(course_id,batch_id,visibility),
 INDEX ix_material_folder(folder_id),
 FOREIGN KEY(course_id) REFERENCES courses(id),
 FOREIGN KEY(batch_id) REFERENCES batches(id),
 FOREIGN KEY(category_id) REFERENCES resource_categories(id),
 FOREIGN KEY(folder_id) REFERENCES resource_folders(id),
 FOREIGN KEY(uploaded_by_id) REFERENCES users(id),
 CHECK(current_version>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS study_material_versions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 material_id BIGINT NOT NULL,
 version_number INT NOT NULL,
 original_file_name VARCHAR(180) NOT NULL,
 storage_key VARCHAR(50) NOT NULL UNIQUE,
 content_type VARCHAR(150) NOT NULL,
 file_size BIGINT NOT NULL,
 uploaded_by_id BIGINT NOT NULL,
 change_notes VARCHAR(2000),
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_material_version(material_id,version_number),
 FOREIGN KEY(material_id) REFERENCES study_materials(id),
 FOREIGN KEY(uploaded_by_id) REFERENCES users(id),
 CHECK(version_number>0),
 CHECK(file_size>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
