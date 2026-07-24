SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `twsny_achievement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `login` varchar(512) NOT NULL,
  `type` varchar(50) NOT NULL,
  `points` bigint DEFAULT 0,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_twsny_achievement_login_type` (`login`, `type`),
  KEY `idx_twsny_achievement_login` (`login`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_achievement` AUTO_INCREMENT = 1000;
