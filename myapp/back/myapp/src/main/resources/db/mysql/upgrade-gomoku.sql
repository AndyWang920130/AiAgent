-- Upgrade: online Gomoku games.
-- Adds the twsny_gomoku_game table backing the "invite a fan to a Gomoku match" feature.
-- A single row models both the invitation (status PENDING) and the match once accepted.
-- Safe to re-run (CREATE TABLE IF NOT EXISTS). Prod runs with ddl-auto=none, so apply manually.

CREATE TABLE IF NOT EXISTS `twsny_gomoku_game` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `black_username` varchar(100) NOT NULL,
  `white_username` varchar(100) NOT NULL,
  `status` varchar(20) NOT NULL,
  `board` varchar(225) NOT NULL,
  `current_player` int NOT NULL DEFAULT 1,
  `winner` int DEFAULT NULL,
  `last_move_row` int DEFAULT NULL,
  `last_move_col` int DEFAULT NULL,
  `move_count` int NOT NULL DEFAULT 0,
  `started_date` datetime(6) DEFAULT NULL,
  `last_move_date` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_twsny_gomoku_game_black` (`black_username`),
  KEY `idx_twsny_gomoku_game_white` (`white_username`),
  KEY `idx_twsny_gomoku_game_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_gomoku_game` AUTO_INCREMENT = 1000;

-- Timeout columns, added idempotently for any DB where twsny_gomoku_game already exists
-- without them (the CREATE TABLE above already includes them for fresh installs).
SET @schema_name = DATABASE();

SET @add_started_date = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_gomoku_game` ADD COLUMN `started_date` datetime(6) DEFAULT NULL AFTER `move_count`',
    'SELECT ''twsny_gomoku_game.started_date already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_gomoku_game'
    AND COLUMN_NAME = 'started_date'
);
PREPARE add_started_date_stmt FROM @add_started_date;
EXECUTE add_started_date_stmt;
DEALLOCATE PREPARE add_started_date_stmt;

SET @add_last_move_date = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_gomoku_game` ADD COLUMN `last_move_date` datetime(6) DEFAULT NULL AFTER `started_date`',
    'SELECT ''twsny_gomoku_game.last_move_date already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_gomoku_game'
    AND COLUMN_NAME = 'last_move_date'
);
PREPARE add_last_move_date_stmt FROM @add_last_move_date;
EXECUTE add_last_move_date_stmt;
DEALLOCATE PREPARE add_last_move_date_stmt;

SET @add_left_by_username = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_gomoku_game` ADD COLUMN `left_by_username` varchar(100) DEFAULT NULL AFTER `winner`',
    'SELECT ''twsny_gomoku_game.left_by_username already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_gomoku_game'
    AND COLUMN_NAME = 'left_by_username'
);
PREPARE add_left_by_username_stmt FROM @add_left_by_username;
EXECUTE add_left_by_username_stmt;
DEALLOCATE PREPARE add_left_by_username_stmt;
