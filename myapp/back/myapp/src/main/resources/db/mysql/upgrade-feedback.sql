-- Upgrade: user feedback to the administrators.
-- Adds the twsny_feedback table backing the Feedback page and the admin triage page. Safe
-- to re-run (CREATE TABLE IF NOT EXISTS). Prod runs with ddl-auto=none, so apply manually.

CREATE TABLE IF NOT EXISTS `twsny_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL,
  `category` varchar(20) NOT NULL DEFAULT 'OTHER',
  `title` varchar(200) NOT NULL,
  `content` varchar(2000) NOT NULL,
  `contact` varchar(200) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'NEW',
  `reply` varchar(2000) DEFAULT NULL,
  `replied_by` varchar(100) DEFAULT NULL,
  `replied_date` datetime(6) DEFAULT NULL,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_twsny_feedback_username` (`username`),
  KEY `idx_twsny_feedback_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_feedback` AUTO_INCREMENT = 1000;
