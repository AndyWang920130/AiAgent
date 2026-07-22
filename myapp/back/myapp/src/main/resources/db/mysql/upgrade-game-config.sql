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
  (1, 'WHEEL_PRIZE', 'Grand Prize', 'red', NULL, 10, 'system'),
  (2, 'WHEEL_PRIZE', 'First Prize', 'orange', NULL, 20, 'system'),
  (3, 'WHEEL_PRIZE', 'Second Prize', 'gold', NULL, 30, 'system'),
  (4, 'WHEEL_PRIZE', 'Third Prize', 'green', NULL, 40, 'system'),
  (5, 'WHEEL_PRIZE', 'Try Again', 'blue', NULL, 50, 'system'),
  (6, 'WHEEL_PRIZE', 'Lucky Draw', 'purple', NULL, 60, 'system'),
  (7, 'PARAMETER', 'spinDurationSeconds', '4', 'How long the wheel spins, in seconds', 10, 'system'),
  (8, 'LIST_PRIZE', 'Student 1', 'red', NULL, 10, 'system'),
  (9, 'LIST_PRIZE', 'Student 2', 'orange', NULL, 20, 'system'),
  (10, 'LIST_PRIZE', 'Student 3', 'gold', NULL, 30, 'system'),
  (11, 'LIST_PRIZE', 'Student 4', 'green', NULL, 40, 'system'),
  (12, 'LIST_PRIZE', 'Student 5', 'cyan', NULL, 50, 'system'),
  (13, 'LIST_PRIZE', 'Student 6', 'blue', NULL, 60, 'system'),
  (14, 'LIST_PRIZE', 'Student 7', 'purple', NULL, 70, 'system'),
  (15, 'LIST_PRIZE', 'Student 8', 'magenta', NULL, 80, 'system'),
  (16, 'LIST_PRIZE', 'Student 9', 'red', NULL, 90, 'system'),
  (17, 'LIST_PRIZE', 'Student 10', 'orange', NULL, 100, 'system'),
  (18, 'LIST_PRIZE', 'Student 11', 'gold', NULL, 110, 'system'),
  (19, 'LIST_PRIZE', 'Student 12', 'green', NULL, 120, 'system'),
  (20, 'LIST_PRIZE', 'Student 13', 'cyan', NULL, 130, 'system'),
  (21, 'LIST_PRIZE', 'Student 14', 'blue', NULL, 140, 'system'),
  (22, 'LIST_PRIZE', 'Student 15', 'purple', NULL, 150, 'system'),
  (23, 'LIST_PRIZE', 'Student 16', 'magenta', NULL, 160, 'system'),
  (24, 'LIST_PRIZE', 'Student 17', 'red', NULL, 170, 'system'),
  (25, 'LIST_PRIZE', 'Student 18', 'orange', NULL, 180, 'system'),
  (26, 'LIST_PRIZE', 'Student 19', 'gold', NULL, 190, 'system'),
  (27, 'LIST_PRIZE', 'Student 20', 'green', NULL, 200, 'system');

ALTER TABLE `twsny_game_config` AUTO_INCREMENT = 1000;
