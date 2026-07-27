SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `twsny_ecg_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `lead_name` varchar(20) DEFAULT NULL,
  `sample_rate` int DEFAULT NULL,
  `heart_rate` int DEFAULT NULL,
  `samples` longtext,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_ecg_record` AUTO_INCREMENT = 1000;
