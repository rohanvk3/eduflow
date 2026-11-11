-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Generated from an isolated MySQL 8.0.46 verification database; no application rows.-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `announcements`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `audience_type` enum('ALL','BATCH','COURSE','ROLE') NOT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `delivery_job_id` bigint DEFAULT NULL,
  `publication_issue` varchar(250) DEFAULT NULL,
  `publish_at` datetime(6) DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `status` enum('ARCHIVED','DRAFT','PUBLISHED','SCHEDULED') NOT NULL,
  `target_role` enum('ADMIN','PARENT','STUDENT','TEACHER') DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `batch_id` bigint DEFAULT NULL,
  `course_id` bigint DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_announcement_schedule` (`status`,`publish_at`),
  KEY `ix_announcement_scope` (`batch_id`,`status`),
  KEY `FKcbrf5570bepj9wk21ofsrpwp2` (`course_id`),
  KEY `FK2xq7ufsyexvej4l6ld869ddxy` (`created_by_id`),
  CONSTRAINT `FK1hxvqm35o0w0l4dy9cavjwmvj` FOREIGN KEY (`batch_id`) REFERENCES `batches` (`id`),
  CONSTRAINT `FK2xq7ufsyexvej4l6ld869ddxy` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKcbrf5570bepj9wk21ofsrpwp2` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assignment_attachments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignment_attachments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `content_type` varchar(120) NOT NULL,
  `file_size` bigint NOT NULL,
  `original_file_name` varchar(180) NOT NULL,
  `storage_key` varchar(60) NOT NULL,
  `assignment_id` bigint NOT NULL,
  `uploaded_by_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlb839qd5wnspbndu06gb31adp` (`assignment_id`),
  KEY `FKeovklt64rnm57vcecs3t8u1k0` (`uploaded_by_id`),
  CONSTRAINT `FKeovklt64rnm57vcecs3t8u1k0` FOREIGN KEY (`uploaded_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKlb839qd5wnspbndu06gb31adp` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assignment_recipients`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignment_recipients` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `assignment_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assignment_recipient` (`assignment_id`,`student_id`),
  KEY `FK3x1775t2fenhs7m9nxy011o5n` (`student_id`),
  CONSTRAINT `FK3x1775t2fenhs7m9nxy011o5n` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FK8uuby2ofq2qhbp7ov334rgsbo` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assignment_submissions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignment_submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `feedback` varchar(5000) DEFAULT NULL,
  `graded_at` datetime(6) DEFAULT NULL,
  `late` bit(1) NOT NULL,
  `obtained_marks` decimal(10,2) DEFAULT NULL,
  `status` enum('GRADED','LATE','SUBMITTED') NOT NULL,
  `submission_text` mediumtext,
  `submitted_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `assignment_id` bigint NOT NULL,
  `graded_by_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assignment_submission` (`assignment_id`,`student_id`),
  KEY `ix_submission_status` (`status`,`submitted_at`),
  KEY `FKhvh6nihwy3hrxy84msvn6wk1l` (`graded_by_id`),
  KEY `FKix5j5yhosm3rle137aqkm6wgn` (`student_id`),
  CONSTRAINT `FKhvh6nihwy3hrxy84msvn6wk1l` FOREIGN KEY (`graded_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKix5j5yhosm3rle137aqkm6wgn` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKm7i7ubgh7y2n6mvg8muw62oax` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assignments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `allow_late_submission` bit(1) NOT NULL,
  `description` text,
  `due_date_time` datetime(6) NOT NULL,
  `max_marks` decimal(10,2) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `status` enum('CLOSED','DRAFT','PUBLISHED') NOT NULL,
  `title` varchar(200) NOT NULL,
  `version` bigint NOT NULL,
  `batch_id` bigint NOT NULL,
  `created_by_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_assignment_batch_due` (`batch_id`,`due_date_time`),
  KEY `FKdst5f4lsfa0i7otgo3t2s87n4` (`created_by_id`),
  CONSTRAINT `FK7j99j00ve3klnvi9gtbwnt39f` FOREIGN KEY (`batch_id`) REFERENCES `batches` (`id`),
  CONSTRAINT `FKdst5f4lsfa0i7otgo3t2s87n4` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attendance_records`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `remarks` varchar(500) DEFAULT NULL,
  `status` enum('ABSENT','EXCUSED','LATE','PRESENT') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `session_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attendance_session_student` (`session_id`,`student_id`),
  KEY `ix_attendance_student_session` (`student_id`,`session_id`),
  CONSTRAINT `FKb5ijilkgrgx66qn66iajdkyb9` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKfaf92mkjrosrvdqq5bev7cl1m` FOREIGN KEY (`session_id`) REFERENCES `attendance_sessions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attendance_sessions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `end_time` time(6) DEFAULT NULL,
  `finalized_at` datetime(6) DEFAULT NULL,
  `occurrence_key` varchar(20) NOT NULL,
  `session_date` date NOT NULL,
  `start_time` time(6) DEFAULT NULL,
  `status` enum('DRAFT','FINALIZED') NOT NULL,
  `topic` varchar(250) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint DEFAULT NULL,
  `batch_id` bigint NOT NULL,
  `marked_by_id` bigint NOT NULL,
  `schedule_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attendance_occurrence` (`batch_id`,`session_date`,`occurrence_key`),
  KEY `ix_attendance_session_date` (`session_date`),
  KEY `ix_attendance_batch_date` (`batch_id`,`session_date`),
  KEY `FK7r6i4ubpo0mehu2uh8kbyom3i` (`marked_by_id`),
  KEY `FK21srgufwo289ukeg54fo5j3xv` (`schedule_id`),
  CONSTRAINT `FK21srgufwo289ukeg54fo5j3xv` FOREIGN KEY (`schedule_id`) REFERENCES `batch_schedules` (`id`),
  CONSTRAINT `FK7r6i4ubpo0mehu2uh8kbyom3i` FOREIGN KEY (`marked_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKb3q9kh6o3eo8vav012p9otpp0` FOREIGN KEY (`batch_id`) REFERENCES `batches` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_logs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(80) NOT NULL,
  `actor_role` varchar(20) DEFAULT NULL,
  `actor_user_id` bigint DEFAULT NULL,
  `description` varchar(500) NOT NULL,
  `entity_id` bigint DEFAULT NULL,
  `entity_type` varchar(60) NOT NULL,
  `outcome` varchar(20) NOT NULL,
  `request_id` varchar(36) DEFAULT NULL,
  `event_time` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_audit_time` (`event_time`,`id`),
  KEY `ix_audit_actor` (`actor_user_id`,`event_time`),
  KEY `ix_audit_action` (`action`,`event_time`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batch_enrollments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_enrollments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `completion_date` date DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `enrollment_date` date NOT NULL,
  `status` enum('ACTIVE','COMPLETED','WITHDRAWN') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `withdrawal_date` date DEFAULT NULL,
  `batch_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKj2cwdlup0rpxu6w0be3iibwfv` (`batch_id`,`student_id`,`enrollment_date`),
  KEY `ix_enrollment_eligibility` (`batch_id`,`student_id`,`enrollment_date`,`withdrawal_date`),
  KEY `FKovy9p04wihkb3x07qnlmq3sa1` (`student_id`),
  CONSTRAINT `FKgps0b71je2arsvbyrfk92ybmq` FOREIGN KEY (`batch_id`) REFERENCES `batches` (`id`),
  CONSTRAINT `FKovy9p04wihkb3x07qnlmq3sa1` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batch_schedules`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') NOT NULL,
  `end_time` time(6) NOT NULL,
  `room` varchar(100) DEFAULT NULL,
  `start_time` time(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `batch_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_schedule_teacher_day` (`teacher_id`,`day_of_week`,`active`),
  KEY `FKgqc1l1hxdycm54n5aqv7xr3rb` (`batch_id`),
  CONSTRAINT `FKevuuad46irbixfoglhl3y5o6f` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`),
  CONSTRAINT `FKgqc1l1hxdycm54n5aqv7xr3rb` FOREIGN KEY (`batch_id`) REFERENCES `batches` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batch_teacher_assignments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_teacher_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `batch_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcyqeldnta936nu5mmg4o0og37` (`batch_id`,`teacher_id`),
  KEY `FK6ucwdgxloojkvpdutpxfgl761` (`teacher_id`),
  CONSTRAINT `FK6ucwdgxloojkvpdutpxfgl761` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`),
  CONSTRAINT `FKoeakstp63d8b0ajly01qr5skw` FOREIGN KEY (`batch_id`) REFERENCES `batches` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batches`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `capacity` int NOT NULL,
  `code` varchar(40) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `end_date` date NOT NULL,
  `name` varchar(150) NOT NULL,
  `start_date` date NOT NULL,
  `status` enum('ACTIVE','CANCELLED','COMPLETED','PLANNED') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `course_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkbybix9e3cxd9yl7crefl9qpe` (`code`),
  KEY `FKfw7md94a64xgsv0otrhnbxh98` (`course_id`),
  CONSTRAINT `FKfw7md94a64xgsv0otrhnbxh98` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `courses`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(40) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `duration_weeks` int NOT NULL,
  `name` varchar(150) NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK61og8rbqdd2y28rx2et5fdnxd` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `exam_answers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_answers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `awarded_marks` decimal(10,2) DEFAULT NULL,
  `attempt_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `selected_option_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_answer_question` (`attempt_id`,`question_id`),
  KEY `FKcgxivsvqp4ns0wtdsd5r1ymma` (`question_id`),
  KEY `FKi7y147d8gq9uskb9fe9vwbhwk` (`selected_option_id`),
  CONSTRAINT `FKcgxivsvqp4ns0wtdsd5r1ymma` FOREIGN KEY (`question_id`) REFERENCES `exam_questions` (`id`),
  CONSTRAINT `FKi7y147d8gq9uskb9fe9vwbhwk` FOREIGN KEY (`selected_option_id`) REFERENCES `exam_options` (`id`),
  CONSTRAINT `FKpcx0r6nsdbcx1pq8h7or1o8bc` FOREIGN KEY (`attempt_id`) REFERENCES `exam_attempts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `exam_attempts`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_attempts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `obtained_marks` decimal(10,2) DEFAULT NULL,
  `started_at` datetime(6) NOT NULL,
  `status` enum('AUTO_SUBMITTED','IN_PROGRESS','SUBMITTED') NOT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL,
  `exam_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_attempt_student` (`exam_id`,`student_id`),
  KEY `ix_attempt_expiry` (`status`,`expires_at`),
  KEY `FKt2bysgtvvg64lgryf27b1xo76` (`student_id`),
  CONSTRAINT `FK5tomiinihc09ywy0wh15pi2cs` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`),
  CONSTRAINT `FKt2bysgtvvg64lgryf27b1xo76` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `exam_options`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_options` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `is_correct` bit(1) NOT NULL,
  `display_order` int NOT NULL,
  `option_text` varchar(2000) NOT NULL,
  `question_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKof6mt6q0fg9b59dtac3ly8rl` (`question_id`),
  CONSTRAINT `FKof6mt6q0fg9b59dtac3ly8rl` FOREIGN KEY (`question_id`) REFERENCES `exam_questions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `exam_questions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_questions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `display_order` int NOT NULL,
  `marks` decimal(10,2) NOT NULL,
  `question_text` varchar(6000) NOT NULL,
  `exam_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5cd6sjmccb11rrwpyabyc81c0` (`exam_id`),
  CONSTRAINT `FK5cd6sjmccb11rrwpyabyc81c0` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `exam_result_corrections`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_result_corrections` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `corrected_at` datetime(6) NOT NULL,
  `new_marks` decimal(10,2) NOT NULL,
  `previous_marks` decimal(10,2) NOT NULL,
  `reason` varchar(1000) NOT NULL,
  `corrected_by_id` bigint NOT NULL,
  `result_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnbgwr002eym48gps654bomiln` (`corrected_by_id`),
  KEY `FKoybf33o8aihgxb5agxa28yjmq` (`result_id`),
  CONSTRAINT `FKnbgwr002eym48gps654bomiln` FOREIGN KEY (`corrected_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKoybf33o8aihgxb5agxa28yjmq` FOREIGN KEY (`result_id`) REFERENCES `exam_results` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `exam_results`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_results` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `evaluated_at` datetime(6) NOT NULL,
  `obtained_marks` decimal(10,2) NOT NULL,
  `remarks` varchar(1000) DEFAULT NULL,
  `evaluated_by_id` bigint DEFAULT NULL,
  `exam_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_result_student` (`exam_id`,`student_id`),
  KEY `FK4yx4sshvvvtfmvqi8id9ayyrf` (`evaluated_by_id`),
  KEY `FKr7qgl670f47u65kkdm8ex5119` (`student_id`),
  CONSTRAINT `FK4yx4sshvvvtfmvqi8id9ayyrf` FOREIGN KEY (`evaluated_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKr7qgl670f47u65kkdm8ex5119` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKtf85ht7yquiorwjx2xbdx3fxw` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `exams`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exams` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `cancellation_reason` varchar(1000) DEFAULT NULL,
  `description` varchar(4000) DEFAULT NULL,
  `duration_minutes` int DEFAULT NULL,
  `eligibility_date` date NOT NULL,
  `end_date_time` datetime(6) NOT NULL,
  `exam_code` varchar(40) NOT NULL,
  `exam_type` enum('OFFLINE','ONLINE_MCQ') NOT NULL,
  `passing_marks` decimal(10,2) NOT NULL,
  `results_published` bit(1) NOT NULL,
  `results_published_at` datetime(6) DEFAULT NULL,
  `start_date_time` datetime(6) NOT NULL,
  `status` enum('CANCELLED','COMPLETED','DRAFT','PUBLISHED') NOT NULL,
  `title` varchar(200) NOT NULL,
  `total_marks` decimal(10,2) NOT NULL,
  `version` bigint NOT NULL,
  `batch_id` bigint NOT NULL,
  `created_by_id` bigint NOT NULL,
  `results_published_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_code` (`exam_code`),
  KEY `ix_exam_batch_time` (`batch_id`,`start_date_time`),
  KEY `FKpb6t4jh679dskd6axk3ogdknk` (`created_by_id`),
  KEY `FK78ruqrd188hay816ukf9cxed8` (`results_published_by_id`),
  CONSTRAINT `FK78ruqrd188hay816ukf9cxed8` FOREIGN KEY (`results_published_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKpb6t4jh679dskd6axk3ogdknk` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKq209f5605e0y2p65a4bhlj2pu` FOREIGN KEY (`batch_id`) REFERENCES `batches` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fee_installments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_installments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `due_date` date NOT NULL,
  `installment_number` int NOT NULL,
  `fee_account_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fee_installment_number` (`fee_account_id`,`installment_number`),
  KEY `ix_fee_installment_due` (`due_date`),
  CONSTRAINT `FKcof5m7l96mfwy4wokw9xxloy4` FOREIGN KEY (`fee_account_id`) REFERENCES `student_fee_accounts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fee_payments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `external_reference_key` varchar(180) DEFAULT NULL,
  `idempotency_key` varchar(100) NOT NULL,
  `payment_date` date NOT NULL,
  `payment_method` enum('BANK_TRANSFER','CASH','CHEQUE','UPI') NOT NULL,
  `receipt_number` varchar(60) NOT NULL,
  `remarks` varchar(1000) DEFAULT NULL,
  `request_hash` varchar(64) NOT NULL,
  `status` enum('SUCCESS','VOIDED') NOT NULL,
  `transaction_reference` varchar(150) DEFAULT NULL,
  `fee_account_id` bigint NOT NULL,
  `recorded_by_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fee_receipt` (`receipt_number`),
  UNIQUE KEY `uk_fee_idempotency` (`idempotency_key`),
  UNIQUE KEY `uk_fee_external_reference` (`external_reference_key`),
  KEY `ix_fee_payment_date_status` (`payment_date`,`status`),
  KEY `FKhl52richadbhi1qp01e1mtm91` (`fee_account_id`),
  KEY `FKecw6ly6af5etgnaldt4mky5c9` (`recorded_by_id`),
  CONSTRAINT `FKecw6ly6af5etgnaldt4mky5c9` FOREIGN KEY (`recorded_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKhl52richadbhi1qp01e1mtm91` FOREIGN KEY (`fee_account_id`) REFERENCES `student_fee_accounts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fee_structures`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fee_structures` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `admission_fee` decimal(14,2) NOT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `material_fee` decimal(14,2) NOT NULL,
  `name` varchar(150) NOT NULL,
  `other_fee` decimal(14,2) NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `total_fee` decimal(14,2) NOT NULL,
  `tuition_fee` decimal(14,2) NOT NULL,
  `course_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_fee_structure_course_dates` (`course_id`,`effective_from`,`effective_to`),
  CONSTRAINT `FKb2emuafexommm3bx0gvbp53x2` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `institute_settings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `institute_settings` (
  `id` bigint NOT NULL,
  `academic_year_end` date DEFAULT NULL,
  `academic_year_start` date DEFAULT NULL,
  `address_line1` varchar(250) DEFAULT NULL,
  `address_line2` varchar(250) DEFAULT NULL,
  `alternate_phone` varchar(16) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `country` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `currency_code` varchar(3) NOT NULL,
  `email` varchar(254) DEFAULT NULL,
  `institute_code` varchar(30) NOT NULL,
  `institute_name` varchar(150) NOT NULL,
  `logo_content_type` varchar(40) DEFAULT NULL,
  `logo_storage_key` varchar(80) DEFAULT NULL,
  `phone_number` varchar(16) DEFAULT NULL,
  `postal_code` varchar(20) DEFAULT NULL,
  `state` varchar(100) DEFAULT NULL,
  `timezone` varchar(80) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL,
  `website` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_coordinators`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_coordinators` (
  `name` varchar(40) NOT NULL,
  `last_id` bigint NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_jobs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_jobs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `complete` bit(1) NOT NULL,
  `context_key` varchar(120) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `delivered_count` bigint NOT NULL,
  `event_key` varchar(180) NOT NULL,
  `processed_count` bigint NOT NULL,
  `recipient_count` bigint NOT NULL,
  `related_id` bigint NOT NULL,
  `related_type` varchar(40) NOT NULL,
  `skipped_count` bigint NOT NULL,
  `type` enum('ANNOUNCEMENT','ASSIGNMENT_CREATED','ASSIGNMENT_DUE','ASSIGNMENT_GRADED','ATTENDANCE_FINALIZED','EXAM_RESULT_PUBLISHED','EXAM_SCHEDULED','FEE_DUE','FEE_OVERDUE','GENERAL','PAYMENT_RECORDED','STUDY_MATERIAL_PUBLISHED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKbhb2588f7kncmp8twfhajmsne` (`event_key`),
  KEY `ix_notification_job_pending` (`complete`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_preferences`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_preferences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `announcements_enabled` bit(1) NOT NULL,
  `assignment_notifications_enabled` bit(1) NOT NULL,
  `attendance_notifications_enabled` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `exam_notifications_enabled` bit(1) NOT NULL,
  `fee_notifications_enabled` bit(1) NOT NULL,
  `material_notifications_enabled` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKn2jopkbm16qv3xelbvoyjkd0g` (`user_id`),
  CONSTRAINT `FKt9qjvmcl36i14utm5uptyqg84` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_targets`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_targets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `delivered` bit(1) NOT NULL,
  `processed` bit(1) NOT NULL,
  `student_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `job_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_target` (`job_id`,`user_id`),
  KEY `ix_target_pending` (`job_id`,`processed`,`id`),
  CONSTRAINT `FKfy0961th58exeug08m3o6etd7` FOREIGN KEY (`job_id`) REFERENCES `notification_jobs` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action_url` varchar(300) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `deduplication_key` varchar(180) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `message` varchar(1000) NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `read_flag` bit(1) NOT NULL,
  `related_entity_id` bigint DEFAULT NULL,
  `related_entity_type` varchar(40) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `type` enum('ANNOUNCEMENT','ASSIGNMENT_CREATED','ASSIGNMENT_DUE','ASSIGNMENT_GRADED','ATTENDANCE_FINALIZED','EXAM_RESULT_PUBLISHED','EXAM_SCHEDULED','FEE_DUE','FEE_OVERDUE','GENERAL','PAYMENT_RECORDED','STUDY_MATERIAL_PUBLISHED') NOT NULL,
  `recipient_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_event` (`recipient_id`,`deduplication_key`),
  KEY `ix_notification_inbox` (`recipient_id`,`deleted_at`,`created_at`),
  KEY `ix_notification_unread` (`recipient_id`,`read_flag`,`deleted_at`),
  CONSTRAINT `FKqqnsjxlwleyjbxlmm213jaj3f` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `parent_link_audits`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parent_link_audits` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `changed_at` datetime(6) NOT NULL,
  `new_status` enum('PENDING','REVOKED','VERIFIED') NOT NULL,
  `previous_status` enum('PENDING','REVOKED','VERIFIED') DEFAULT NULL,
  `primary_guardian` bit(1) NOT NULL,
  `reason` varchar(1000) NOT NULL,
  `relationship_type` enum('FATHER','GUARDIAN','MOTHER','OTHER') NOT NULL,
  `changed_by_id` bigint NOT NULL,
  `link_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKibmk2f5lns8lgeg4st8cs615y` (`changed_by_id`),
  KEY `FK64icpb8e2rpfx35xuhr849xj5` (`link_id`),
  CONSTRAINT `FK64icpb8e2rpfx35xuhr849xj5` FOREIGN KEY (`link_id`) REFERENCES `parent_student_links` (`id`),
  CONSTRAINT `FKibmk2f5lns8lgeg4st8cs615y` FOREIGN KEY (`changed_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `parent_profiles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parent_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `address` varchar(500) DEFAULT NULL,
  `alternate_phone` varchar(16) DEFAULT NULL,
  `contact_email` varchar(254) DEFAULT NULL,
  `phone_number` varchar(16) NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `version` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1kwvop6254by3rrqmfh5w2oq5` (`user_id`),
  CONSTRAINT `FKhx9yojtdt6tocy5d7cbbst0jr` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `parent_student_links`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parent_student_links` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `primary_guardian` bit(1) NOT NULL,
  `relationship_type` enum('FATHER','GUARDIAN','MOTHER','OTHER') NOT NULL,
  `status` enum('PENDING','REVOKED','VERIFIED') NOT NULL,
  `verified_at` datetime(6) DEFAULT NULL,
  `version` bigint NOT NULL,
  `parent_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `verified_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parent_child` (`parent_id`,`student_id`),
  KEY `ix_parent_link_access` (`parent_id`,`status`,`student_id`),
  KEY `FK75kc9sb7vtllyup6dqav9k1su` (`student_id`),
  KEY `FK2j1mp3erj66wkntkw208qatx7` (`verified_by_id`),
  CONSTRAINT `FK2j1mp3erj66wkntkw208qatx7` FOREIGN KEY (`verified_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK6gp4gsrjhxg87epiq0kg74mnr` FOREIGN KEY (`parent_id`) REFERENCES `parent_profiles` (`id`),
  CONSTRAINT `FK75kc9sb7vtllyup6dqav9k1su` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `password_reset_tokens`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_tokens` (
  `token_hash` varchar(64) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `security_version` bigint NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`token_hash`),
  KEY `ix_reset_user` (`user_id`,`expires_at`),
  CONSTRAINT `FKk3ndxg5xp6v7wd4gjyusp15gq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_allocations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_allocations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `installment_id` bigint NOT NULL,
  `payment_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_installment` (`payment_id`,`installment_id`),
  KEY `FKch69bi9ggsljdn72ryvjt3ods` (`installment_id`),
  CONSTRAINT `FKch69bi9ggsljdn72ryvjt3ods` FOREIGN KEY (`installment_id`) REFERENCES `fee_installments` (`id`),
  CONSTRAINT `FKjftfnyur38j6fx36gq89pjq81` FOREIGN KEY (`payment_id`) REFERENCES `fee_payments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_void_audits`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_void_audits` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `new_status` varchar(20) NOT NULL,
  `previous_amount` decimal(14,2) NOT NULL,
  `previous_status` varchar(20) NOT NULL,
  `reason` varchar(1000) NOT NULL,
  `changed_by_id` bigint NOT NULL,
  `payment_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_void` (`payment_id`),
  KEY `FKf08ji6c4redimht3grktmsvub` (`changed_by_id`),
  CONSTRAINT `FK21n5nuwhy5b6g5nmgssdnrgvo` FOREIGN KEY (`payment_id`) REFERENCES `fee_payments` (`id`),
  CONSTRAINT `FKf08ji6c4redimht3grktmsvub` FOREIGN KEY (`changed_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recovery_throttles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recovery_throttles` (
  `client_hash` varchar(64) NOT NULL,
  `next_allowed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`client_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_categories`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `icon` varchar(40) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `normalized_name` varchar(100) NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKxuslo8us6s6uu0iy1dh3iygr` (`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_folders`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resource_folders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `description` varchar(2000) DEFAULT NULL,
  `name` varchar(150) NOT NULL,
  `batch_id` bigint DEFAULT NULL,
  `course_id` bigint NOT NULL,
  `created_by_id` bigint NOT NULL,
  `parent_folder_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4upgi52lh5hxoblnt41bdi3hj` (`batch_id`),
  KEY `FKb5g8ytbtl2d2o70qtu0oi9ogx` (`course_id`),
  KEY `FKh9vvibb443dbgs5w7g0c5d6na` (`created_by_id`),
  KEY `FKitpwgj7218qddujmfpfocvcmn` (`parent_folder_id`),
  CONSTRAINT `FK4upgi52lh5hxoblnt41bdi3hj` FOREIGN KEY (`batch_id`) REFERENCES `batches` (`id`),
  CONSTRAINT `FKb5g8ytbtl2d2o70qtu0oi9ogx` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `FKh9vvibb443dbgs5w7g0c5d6na` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKitpwgj7218qddujmfpfocvcmn` FOREIGN KEY (`parent_folder_id`) REFERENCES `resource_folders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student_fee_accounts`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_fee_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `admission_fee` decimal(14,2) NOT NULL,
  `agreement_date` date NOT NULL,
  `discount_amount` decimal(14,2) NOT NULL,
  `final_payable_amount` decimal(14,2) NOT NULL,
  `material_fee` decimal(14,2) NOT NULL,
  `original_amount` decimal(14,2) NOT NULL,
  `other_fee` decimal(14,2) NOT NULL,
  `status` enum('ACTIVE','CANCELLED','CLOSED') NOT NULL,
  `structure_name` varchar(150) NOT NULL,
  `tuition_fee` decimal(14,2) NOT NULL,
  `version` bigint NOT NULL,
  `enrollment_id` bigint NOT NULL,
  `fee_structure_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fee_account_enrollment` (`enrollment_id`),
  KEY `ix_fee_account_student` (`student_id`),
  KEY `FKdg1n0i3cgc0as5f2lxcwl5v0j` (`fee_structure_id`),
  CONSTRAINT `FK2qgca80kr44rq77ugayruk11f` FOREIGN KEY (`enrollment_id`) REFERENCES `batch_enrollments` (`id`),
  CONSTRAINT `FK6miw9ffhlyna4cr5la53joswr` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKdg1n0i3cgc0as5f2lxcwl5v0j` FOREIGN KEY (`fee_structure_id`) REFERENCES `fee_structures` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `students`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(500) NOT NULL,
  `admission_date` date NOT NULL,
  `admission_number` varchar(40) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `date_of_birth` date NOT NULL,
  `email` varchar(254) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `gender` varchar(30) DEFAULT NULL,
  `guardian_name` varchar(100) NOT NULL,
  `guardian_phone` varchar(16) NOT NULL,
  `phone` varchar(16) NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_students_admission_number` (`admission_number`),
  UNIQUE KEY `uk_students_email` (`email`),
  UNIQUE KEY `uk_students_user` (`user_id`),
  CONSTRAINT `fk_students_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `study_material_versions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_material_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `change_notes` varchar(2000) DEFAULT NULL,
  `content_type` varchar(150) NOT NULL,
  `file_size` bigint NOT NULL,
  `original_file_name` varchar(180) NOT NULL,
  `storage_key` varchar(50) NOT NULL,
  `version_number` int NOT NULL,
  `material_id` bigint NOT NULL,
  `uploaded_by_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2e1cf5cal72j1wsu5xmo1ycdv` (`material_id`,`version_number`),
  UNIQUE KEY `UKo3r7smljclqjnfquuq9n12yjt` (`storage_key`),
  KEY `FK2in9iuw56kw43ktpocu5p16y3` (`uploaded_by_id`),
  CONSTRAINT `FK2in9iuw56kw43ktpocu5p16y3` FOREIGN KEY (`uploaded_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKmfkyqsh3n9gtl2vvab5v6xgmr` FOREIGN KEY (`material_id`) REFERENCES `study_materials` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `study_materials`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_materials` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `current_version` int NOT NULL,
  `description` text,
  `external_url` varchar(2000) DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `published_version` int DEFAULT NULL,
  `resource_type` enum('DOCUMENT','IMAGE','LINK','PRESENTATION') NOT NULL,
  `title` varchar(180) NOT NULL,
  `version` bigint NOT NULL,
  `visibility` enum('ARCHIVED','DRAFT','PUBLISHED') NOT NULL,
  `batch_id` bigint DEFAULT NULL,
  `category_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `folder_id` bigint DEFAULT NULL,
  `uploaded_by_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_material_scope` (`course_id`,`batch_id`,`visibility`),
  KEY `ix_material_folder` (`folder_id`),
  KEY `FKb9i1h17tfagivofmp2ng3cm1x` (`batch_id`),
  KEY `FKr29nt6pyyury8jvp6ffko34cw` (`category_id`),
  KEY `FKgc1114nrhgi46y417lp860p18` (`uploaded_by_id`),
  CONSTRAINT `FKb9i1h17tfagivofmp2ng3cm1x` FOREIGN KEY (`batch_id`) REFERENCES `batches` (`id`),
  CONSTRAINT `FKc45bt6cftsh5wdmmic1vask28` FOREIGN KEY (`folder_id`) REFERENCES `resource_folders` (`id`),
  CONSTRAINT `FKcy5p3fh9nkbb1krv84dbcxlcy` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `FKgc1114nrhgi46y417lp860p18` FOREIGN KEY (`uploaded_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKr29nt6pyyury8jvp6ffko34cw` FOREIGN KEY (`category_id`) REFERENCES `resource_categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `submission_attachments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submission_attachments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `active` bit(1) NOT NULL,
  `content_type` varchar(120) NOT NULL,
  `file_size` bigint NOT NULL,
  `original_file_name` varchar(180) NOT NULL,
  `storage_key` varchar(60) NOT NULL,
  `submission_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKelegstvd2yxy3yjai0731l4sc` (`submission_id`),
  CONSTRAINT `FKelegstvd2yxy3yjai0731l4sc` FOREIGN KEY (`submission_id`) REFERENCES `assignment_submissions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `submission_grade_history`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submission_grade_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `graded_at` datetime(6) NOT NULL,
  `new_feedback` varchar(5000) DEFAULT NULL,
  `new_marks` decimal(10,2) NOT NULL,
  `previous_feedback` varchar(5000) DEFAULT NULL,
  `previous_marks` decimal(10,2) DEFAULT NULL,
  `reason` varchar(1000) NOT NULL,
  `graded_by_id` bigint NOT NULL,
  `submission_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9v73v1ixd5a991glqj5p41prq` (`graded_by_id`),
  KEY `FKif7s3y6i4xrsmbqhaatkdefvo` (`submission_id`),
  CONSTRAINT `FK9v73v1ixd5a991glqj5p41prq` FOREIGN KEY (`graded_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKif7s3y6i4xrsmbqhaatkdefvo` FOREIGN KEY (`submission_id`) REFERENCES `assignment_submissions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `submission_revisions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submission_revisions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `attachment_ids` varchar(2000) NOT NULL,
  `changed_at` datetime(6) NOT NULL,
  `revision_number` bigint NOT NULL,
  `status` enum('GRADED','LATE','SUBMITTED') NOT NULL,
  `submission_text` mediumtext,
  `submitted_at` datetime(6) NOT NULL,
  `changed_by_id` bigint NOT NULL,
  `submission_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_submission_revision` (`submission_id`,`revision_number`),
  KEY `FKfr8b9g4nnromk3ck1h81th84` (`changed_by_id`),
  CONSTRAINT `FKfr8b9g4nnromk3ck1h81th84` FOREIGN KEY (`changed_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKs7e0aa955gvct1lp6jopemq1e` FOREIGN KEY (`submission_id`) REFERENCES `assignment_submissions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `teacher_subjects`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_subjects` (
  `teacher_id` bigint NOT NULL,
  `subject` varchar(100) NOT NULL,
  PRIMARY KEY (`teacher_id`,`subject`),
  CONSTRAINT `FK6dcl3ihufp4v0j1fuxlw4ksoj` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `teachers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teachers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `date_of_birth` date DEFAULT NULL,
  `email` varchar(254) NOT NULL,
  `employee_id` varchar(50) NOT NULL,
  `experience_years` int DEFAULT NULL,
  `full_name` varchar(100) NOT NULL,
  `gender` varchar(30) DEFAULT NULL,
  `joining_date` date DEFAULT NULL,
  `phone` varchar(16) DEFAULT NULL,
  `qualification` varchar(200) DEFAULT NULL,
  `salary` decimal(12,2) DEFAULT NULL,
  `specialization` varchar(200) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4l9jjfvsct1dd5aufnurxcvbs` (`email`),
  UNIQUE KEY `UK8xdh0jsitskwq83arwxvyihhc` (`employee_id`),
  UNIQUE KEY `uk_teacher_user` (`user_id`),
  CONSTRAINT `FKb8dct7w2j1vl1r2bpstw5isc0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(254) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `password` varchar(60) NOT NULL,
  `reset_requested_at` datetime(6) DEFAULT NULL,
  `role` enum('ADMIN','PARENT','STUDENT','TEACHER') NOT NULL,
  `security_version` bigint NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-11 12:56:49
