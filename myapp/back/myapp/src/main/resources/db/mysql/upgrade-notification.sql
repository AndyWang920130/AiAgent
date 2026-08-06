-- Upgrade: per-user notifications.
-- Adds the twsny_notification table backing the notification bell. Safe to re-run
-- (CREATE TABLE IF NOT EXISTS). Prod runs with ddl-auto=none, so apply manually.

CREATE TABLE IF NOT EXISTS `twsny_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipient` varchar(100) NOT NULL,
  `type` varchar(20) NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` varchar(500) DEFAULT NULL,
  `link` varchar(200) DEFAULT NULL,
  `is_read` bit(1) NOT NULL DEFAULT b'0',
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_twsny_notification_recipient` (`recipient`),
  KEY `idx_twsny_notification_recipient_read` (`recipient`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_notification` AUTO_INCREMENT = 1000;
