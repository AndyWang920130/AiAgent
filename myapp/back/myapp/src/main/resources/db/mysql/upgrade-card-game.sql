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
-- ADD COLUMN IF NOT EXISTS, so each is guarded on information_schema the way
-- upgrade-gomoku.sql guards its own late additions. A bare ALTER is not harmless here: it
-- raises 1060 and the client stops, skipping every statement after it -- which is exactly the
-- database that has these three columns already and is missing only `ready` below.
SET @schema_name = DATABASE();

SET @add_trick_log = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_card_game` ADD COLUMN `trick_log` varchar(512) DEFAULT '''' AFTER `passed_usernames`',
    'SELECT ''twsny_card_game.trick_log already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_card_game'
    AND COLUMN_NAME = 'trick_log'
);
PREPARE add_trick_log_stmt FROM @add_trick_log;
EXECUTE add_trick_log_stmt;
DEALLOCATE PREPARE add_trick_log_stmt;

SET @add_trick_log_number = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_card_game` ADD COLUMN `trick_log_number` int NOT NULL DEFAULT 0 AFTER `trick_log`',
    'SELECT ''twsny_card_game.trick_log_number already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_card_game'
    AND COLUMN_NAME = 'trick_log_number'
);
PREPARE add_trick_log_number_stmt FROM @add_trick_log_number;
EXECUTE add_trick_log_number_stmt;
DEALLOCATE PREPARE add_trick_log_number_stmt;

-- Also added after the first release: auto-play. A player can hand their turns to the server,
-- which then plays that seat with the same brain the CPU seats use. Per seat and per game, so
-- it lapses with the table; defaults off, which is how every existing row should read.
-- Guarded like the two statements above.
SET @add_auto_play = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_card_game_player` ADD COLUMN `auto_play` bit(1) NOT NULL DEFAULT b''0'' AFTER `cpu`',
    'SELECT ''twsny_card_game_player.auto_play already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_card_game_player'
    AND COLUMN_NAME = 'auto_play'
);
PREPARE add_auto_play_stmt FROM @add_auto_play;
EXECUTE add_auto_play_stmt;
DEALLOCATE PREPARE add_auto_play_stmt;

-- And after that: playing again without rebuilding the table. A game that ends now leaves the
-- row in the new REMATCH status — seats still filled, last result standing — and `ready` is how
-- each player says they want the next one. Both are per game and reset by every deal, so
-- defaults off is right for existing rows (all of which are in a terminal status already, where
-- the column is never read). `status` is a varchar(20) and already holds the new value.
-- Guarded like the statements above. `auto_play` is added before this runs, so the AFTER
-- clause has its anchor on a database that had neither column.
SET @add_ready = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_card_game_player` ADD COLUMN `ready` bit(1) NOT NULL DEFAULT b''0'' AFTER `auto_play`',
    'SELECT ''twsny_card_game_player.ready already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_card_game_player'
    AND COLUMN_NAME = 'ready'
);
PREPARE add_ready_stmt FROM @add_ready;
EXECUTE add_ready_stmt;
DEALLOCATE PREPARE add_ready_stmt;

-- And after that: the predefined phrases players send each other during a game. `phrase` holds
-- the code (HELLO, NICE_PLAY, ...), never a sentence -- the app is bilingual, so each client
-- renders the code through its own locale file. `phrase_date` is when it was sent, which the
-- server reads for two things: how long the phrase is still reported for, and how soon that seat
-- may send another.
--
-- Both nullable with no default, unlike the bit(1) flags above: a seat that has said nothing has
-- no phrase, which is an absence rather than a value. Guarded like the statements above, and
-- anchored AFTER `ready`, which the block above has already added by the time these run.
SET @add_phrase = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_card_game_player` ADD COLUMN `phrase` varchar(20) DEFAULT NULL AFTER `ready`',
    'SELECT ''twsny_card_game_player.phrase already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_card_game_player'
    AND COLUMN_NAME = 'phrase'
);
PREPARE add_phrase_stmt FROM @add_phrase;
EXECUTE add_phrase_stmt;
DEALLOCATE PREPARE add_phrase_stmt;

SET @add_phrase_date = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_card_game_player` ADD COLUMN `phrase_date` datetime(6) DEFAULT NULL AFTER `phrase`',
    'SELECT ''twsny_card_game_player.phrase_date already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_card_game_player'
    AND COLUMN_NAME = 'phrase_date'
);
PREPARE add_phrase_date_stmt FROM @add_phrase_date;
EXECUTE add_phrase_date_stmt;
DEALLOCATE PREPARE add_phrase_date_stmt;

-- And after that: consecutive turns the server has taken for a seat because its clock ran out.
-- At two in a row the service switches that seat's `auto_play` on, on the grounds that a player
-- who has let two turns go is away from the table; they keep the switch, and turning it off
-- clears this back to 0. Reset by any turn the player takes themselves and by every deal, so it
-- is per seat and per game like the two flags above it.
--
-- NOT NULL DEFAULT 0 rather than nullable: a seat has always missed some countable number of
-- turns in a row, and for every existing row that number is none. Guarded like the statements
-- above, and anchored AFTER `phrase_date`, which the block above has already added.
SET @add_timeout_streak = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `twsny_card_game_player` ADD COLUMN `timeout_streak` int NOT NULL DEFAULT 0 AFTER `phrase_date`',
    'SELECT ''twsny_card_game_player.timeout_streak already exists'' AS message'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'twsny_card_game_player'
    AND COLUMN_NAME = 'timeout_streak'
);
PREPARE add_timeout_streak_stmt FROM @add_timeout_streak;
EXECUTE add_timeout_streak_stmt;
DEALLOCATE PREPARE add_timeout_streak_stmt;
