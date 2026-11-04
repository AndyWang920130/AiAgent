-- Upgrade: online 70235 card game (七王五二三).
-- Adds twsny_card_game (the table, its invitation phase and the match in one row) and
-- twsny_card_game_player (one row per seat, human or CPU, holding that player's hidden hand).
-- Safe to re-run (CREATE TABLE IF NOT EXISTS). Prod runs with ddl-auto=none, so apply manually.
--
-- Card encoding: two characters per card, rank then suit. Ranks 4 6 8 9 T J Q K A 5 3 2 7
-- plus X for a joker; suits S H D C, or B/L in the suit slot for the big/little joker.
-- Every card column is therefore read by chunking the string into pairs.

CREATE TABLE IF NOT EXISTS `twsny_card_game` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `status` varchar(20) NOT NULL,
  `host_username` varchar(100) NOT NULL,
  `deck` varchar(255) DEFAULT '',
  `current_play` varchar(64) DEFAULT NULL,
  `current_play_username` varchar(100) DEFAULT NULL,
  `current_shape` varchar(16) DEFAULT NULL,
  `current_rank` int DEFAULT NULL,
  `trick_pile` varchar(512) DEFAULT '',
  `passed_usernames` varchar(512) DEFAULT '',
  `trick_log` varchar(512) DEFAULT '',
  `trick_log_number` int NOT NULL DEFAULT 0,
  `current_turn_username` varchar(100) DEFAULT NULL,
  `trick_number` int NOT NULL DEFAULT 0,
  `first_out_username` varchar(100) DEFAULT NULL,
  `winner_username` varchar(100) DEFAULT NULL,
  `left_by_username` varchar(100) DEFAULT NULL,
  `started_date` datetime(6) DEFAULT NULL,
  `last_action_date` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_twsny_card_game_host` (`host_username`),
  KEY `idx_twsny_card_game_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_card_game` AUTO_INCREMENT = 1000;

CREATE TABLE IF NOT EXISTS `twsny_card_game_player` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `game_id` bigint NOT NULL,
  `username` varchar(100) NOT NULL,
  `seat` int NOT NULL,
  `hand` varchar(255) DEFAULT '',
  `score` int NOT NULL DEFAULT 0,
  `cpu` bit(1) NOT NULL DEFAULT b'0',
  `invitation_status` varchar(20) NOT NULL,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_twsny_card_game_player` (`game_id`, `username`),
  KEY `idx_twsny_card_game_player_game` (`game_id`),
  KEY `idx_twsny_card_game_player_user` (`username`),
  CONSTRAINT `fk_twsny_card_game_player_game`
    FOREIGN KEY (`game_id`) REFERENCES `twsny_card_game` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_card_game_player` AUTO_INCREMENT = 1000;

-- Added after the first release of this script: the per-trick play log behind the "8 raised to
-- 9 raised to 10" history the table shows, and the trick number that log describes (it lags
-- trick_number by one just after a trick is scored, because a trick's winning play is the last
-- entry in its own history). CREATE TABLE IF NOT EXISTS above skips an existing table, so a
-- database created by the earlier version needs these columns added here. MySQL has no
-- ADD COLUMN IF NOT EXISTS, so each errors harmlessly (1060, duplicate column) once applied.
ALTER TABLE `twsny_card_game` ADD COLUMN `trick_log` varchar(512) DEFAULT '' AFTER `passed_usernames`;
ALTER TABLE `twsny_card_game` ADD COLUMN `trick_log_number` int NOT NULL DEFAULT 0 AFTER `trick_log`;

-- Also added after the first release: auto-play. A player can hand their turns to the server,
-- which then plays that seat with the same brain the CPU seats use. Per seat and per game, so
-- it lapses with the table; defaults off, which is how every existing row should read.
-- Same caveat as the two statements above — errors harmlessly (1060) once applied.
ALTER TABLE `twsny_card_game_player` ADD COLUMN `auto_play` bit(1) NOT NULL DEFAULT b'0' AFTER `cpu`;
