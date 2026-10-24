-- =============================================================================
-- EzExam Database Schema
-- Compatible with MySQL 8.x / 9.x / 26.x
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `ezexam` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `ezexam`;

-- -----------------------------------------------------------------------------
-- 1. Users Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `role` ENUM('admin', 'student') NOT NULL DEFAULT 'student',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Exams Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exams` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `duration_minutes` INT NOT NULL DEFAULT 30,
    `total_marks` INT NOT NULL DEFAULT 20,
    `passing_percentage` INT NOT NULL DEFAULT 40,
    `negative_mark` DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    `status` ENUM('draft', 'published') NOT NULL DEFAULT 'draft',
    `start_time` DATETIME NULL,
    `end_time` DATETIME NULL,
    `created_by` INT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`created_by`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    INDEX `idx_exams_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Questions Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `questions` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` INT NOT NULL,
    `question_text` TEXT NOT NULL,
    `marks` INT NOT NULL DEFAULT 1,
    `negative_marks` DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`exam_id`) REFERENCES `exams`(`id`) ON DELETE CASCADE,
    INDEX `idx_questions_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Options Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `options` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `question_id` INT NOT NULL,
    `text` TEXT NOT NULL,
    `is_correct` BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (`question_id`) REFERENCES `questions`(`id`) ON DELETE CASCADE,
    INDEX `idx_options_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. Attempts Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `attempts` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `exam_id` INT NOT NULL,
    `status` ENUM('in-progress', 'submitted') NOT NULL DEFAULT 'in-progress',
    `start_time` DATETIME NOT NULL,
    `submitted_at` DATETIME NULL,
    `score` DECIMAL(6, 2) NULL,
    `total` INT NULL,
    `correct` INT NULL,
    `wrong` INT NULL,
    `unanswered` INT NULL,
    `percentage` DECIMAL(5, 2) NULL,
    `passed` BOOLEAN NULL,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`exam_id`) REFERENCES `exams`(`id`) ON DELETE CASCADE,
    INDEX `idx_attempts_user_exam` (`user_id`, `exam_id`),
    INDEX `idx_attempts_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. Attempt Answers Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `attempt_answers` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `attempt_id` INT NOT NULL,
    `question_id` INT NOT NULL,
    `selected_option_id` INT NULL,
    `is_correct` BOOLEAN NOT NULL DEFAULT FALSE,
    `marks_awarded` DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    FOREIGN KEY (`attempt_id`) REFERENCES `attempts`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_id`) REFERENCES `questions`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`selected_option_id`) REFERENCES `options`(`id`) ON DELETE SET NULL,
    INDEX `idx_attempt_answers_attempt` (`attempt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
