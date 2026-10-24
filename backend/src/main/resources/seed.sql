-- =============================================================================
-- EzExam Demo Seed Data (Safe, Non-Destructive)
-- Uses INSERT IGNORE so existing data is never truncated or overwritten
-- Mirrors frontend/src/data/mockData.js
-- =============================================================================

USE `ezexam`;

-- -----------------------------------------------------------------------------
-- 1. Demo Users (admin123 and student123 hashed via BCrypt)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `users` (`id`, `name`, `email`, `password`, `role`) VALUES
(1, 'Admin User', 'admin@ezexam.com', '$2a$10$KQCXwjG3/oyc2U/0b.759ug4K8vLdRFBxBN8NuORPfZAq5Kn1oTwS', 'admin'),
(2, 'Aarav Sharma', 'student@ezexam.com', '$2a$10$9jx6v3He7LUPgbUu4TI5d.K9eI9VOMe/qZiifJ.Mq2UNFvLUU6CVO', 'student');

-- -----------------------------------------------------------------------------
-- 2. Demo Exams
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `exams` (`id`, `title`, `description`, `duration_minutes`, `total_marks`, `passing_percentage`, `negative_mark`, `status`, `start_time`, `end_time`, `created_by`) VALUES
(1, 'Data Structures Mid-Term', 'Stacks, queues, trees, graphs and time-complexity basics.', 30, 20, 40, 0.25, 'published', '2026-09-01 09:00:00', '2026-12-31 23:59:59', 1),
(2, 'Java Programming Quiz', 'OOP concepts, collections, exception handling and JVM basics.', 20, 10, 40, 0.00, 'published', '2026-09-15 09:00:00', '2026-12-31 23:59:59', 1),
(3, 'DBMS Fundamentals', 'ER models, normalization, SQL queries and transactions. Draft — not visible to students.', 45, 15, 50, 0.50, 'draft', '2026-10-10 09:00:00', '2026-12-31 23:59:59', 1);

-- -----------------------------------------------------------------------------
-- 3. Demo Questions
-- -----------------------------------------------------------------------------
-- Exam 1: Data Structures
INSERT IGNORE INTO `questions` (`id`, `exam_id`, `question_text`, `marks`, `negative_marks`) VALUES
(101, 1, 'Which data structure follows FIFO?', 2, 0.25),
(102, 1, 'What is the time complexity of binary search?', 2, 0.25),
(103, 1, 'Which structure uses LIFO?', 2, 0.25),
(104, 1, 'In a BST, inorder traversal yields nodes in…', 2, 0.25),
(105, 1, 'Which traversal is used for level-order?', 2, 0.25);

-- Exam 2: Java Programming
INSERT IGNORE INTO `questions` (`id`, `exam_id`, `question_text`, `marks`, `negative_marks`) VALUES
(201, 2, 'Which keyword is used to inherit a class in Java?', 2, 0.00),
(202, 2, 'Which collection does not allow duplicates?', 2, 0.00),
(203, 2, 'What is the default value of an int field?', 2, 0.00);

-- -----------------------------------------------------------------------------
-- 4. Demo Options
-- -----------------------------------------------------------------------------
-- Options for Q101
INSERT IGNORE INTO `options` (`id`, `question_id`, `text`, `is_correct`) VALUES
(1001, 101, 'Stack', FALSE),
(1002, 101, 'Queue', TRUE),
(1003, 101, 'Tree', FALSE),
(1004, 101, 'Graph', FALSE);

-- Options for Q102
INSERT IGNORE INTO `options` (`id`, `question_id`, `text`, `is_correct`) VALUES
(1005, 102, 'O(n)', FALSE),
(1006, 102, 'O(log n)', TRUE),
(1007, 102, 'O(n²)', FALSE),
(1008, 102, 'O(1)', FALSE);

-- Options for Q103
INSERT IGNORE INTO `options` (`id`, `question_id`, `text`, `is_correct`) VALUES
(1009, 103, 'Queue', FALSE),
(1010, 103, 'Stack', TRUE),
(1011, 103, 'Heap', FALSE),
(1012, 103, 'Linked List', FALSE);

-- Options for Q104
INSERT IGNORE INTO `options` (`id`, `question_id`, `text`, `is_correct`) VALUES
(1013, 104, 'Random order', FALSE),
(1014, 104, 'Sorted order', TRUE),
(1015, 104, 'Reverse order', FALSE),
(1016, 104, 'Level order', FALSE);

-- Options for Q105
INSERT IGNORE INTO `options` (`id`, `question_id`, `text`, `is_correct`) VALUES
(1017, 105, 'DFS', FALSE),
(1018, 105, 'BFS', TRUE),
(1019, 105, 'Inorder', FALSE),
(1020, 105, 'Postorder', FALSE);

-- Options for Q201
INSERT IGNORE INTO `options` (`id`, `question_id`, `text`, `is_correct`) VALUES
(2001, 201, 'implements', FALSE),
(2002, 201, 'extends', TRUE),
(2003, 201, 'inherits', FALSE),
(2004, 201, 'super', FALSE);

-- Options for Q202
INSERT IGNORE INTO `options` (`id`, `question_id`, `text`, `is_correct`) VALUES
(2005, 202, 'ArrayList', FALSE),
(2006, 202, 'LinkedList', FALSE),
(2007, 202, 'HashSet', TRUE),
(2008, 202, 'Vector', FALSE);

-- Options for Q203
INSERT IGNORE INTO `options` (`id`, `question_id`, `text`, `is_correct`) VALUES
(2009, 203, 'null', FALSE),
(2010, 203, '0', TRUE),
(2011, 203, 'undefined', FALSE),
(2012, 203, '1', FALSE);

-- -----------------------------------------------------------------------------
-- 5. Demo Completed Attempt
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `attempts` (`id`, `user_id`, `exam_id`, `status`, `start_time`, `submitted_at`, `score`, `total`, `correct`, `wrong`, `unanswered`, `percentage`, `passed`) VALUES
(9001, 2, 2, 'submitted', '2026-09-28 10:00:00', '2026-09-28 10:12:00', 4.00, 6, 2, 1, 0, 66.67, TRUE);
