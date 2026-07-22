CREATE TABLE IF NOT EXISTS `twsny_game_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `config_value` varchar(50) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `sort_order` int DEFAULT 0,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_twsny_game_config_type_name` (`type`, `name`),
  KEY `idx_twsny_game_config_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `twsny_game_config`
  (`id`, `type`, `name`, `config_value`, `description`, `sort_order`, `created_by`)
VALUES
  (1, 'PRIZE', '🎉 Grand Prize', 'red', NULL, 10, 'system'),
  (2, 'PRIZE', '🥇 First Prize', 'orange', NULL, 20, 'system'),
  (3, 'PRIZE', '🥈 Second Prize', 'gold', NULL, 30, 'system'),
  (4, 'PRIZE', '🥉 Third Prize', 'green', NULL, 40, 'system'),
  (5, 'PRIZE', 'Try Again', 'blue', NULL, 50, 'system'),
  (6, 'PRIZE', 'Lucky Draw', 'purple', NULL, 60, 'system'),
  (7, 'PARAMETER', 'spinDurationSeconds', '4', 'How long the wheel spins, in seconds', 10, 'system');

ALTER TABLE `twsny_game_config` AUTO_INCREMENT = 1000;
