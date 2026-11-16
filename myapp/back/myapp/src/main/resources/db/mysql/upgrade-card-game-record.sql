-- Upgrade: 70235 match records for the administrators' match record list.
--
-- Adds twsny_card_game_record (one row per finished match) and
-- twsny_card_game_record_player (one row per seat, holding that player's final score).
-- Safe to re-run (CREATE TABLE IF NOT EXISTS). Prod runs with ddl-auto=none, so apply manually.
--
-- Why a separate table rather than reading twsny_card_game: that row is the *room*, not the
-- match. A rematch re-deals the same row -- every seat's score goes back to 0, started_date is
-- re-stamped and winner_username is cleared -- so the room only ever holds the most recent
-- game. There is also no end-of-match timestamp on it at all. A match that is to be listed
-- later therefore has to be written down when it ends, which is what these two tables are for.
--
-- Not backfilled on purpose. Existing terminal rooms hold the scores of their most recent game,
-- but their end time could only be approximated from last_modified_date, and every game before
-- the last one in each room is already gone. The list starts empty and fills from here, so
-- every row in it has a true recorded end time.

CREATE TABLE IF NOT EXISTS `twsny_card_game_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `game_id` bigint NOT NULL,
  `host_username` varchar(100) NOT NULL,
  `started_date` datetime(6) NOT NULL,
  `ended_date` datetime(6) NOT NULL,
  `outcome` varchar(20) NOT NULL,
  `winner_username` varchar(100) DEFAULT NULL,
  `first_out_username` varchar(100) DEFAULT NULL,
  `left_by_username` varchar(100) DEFAULT NULL,
  `trick_count` int NOT NULL DEFAULT 0,
  `player_count` int NOT NULL DEFAULT 0,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  -- One record per deal, many per room. Each deal re-stamps started_date, so the pair is a
  -- natural identity for "this particular match" -- which makes a repeated write a detectable
  -- duplicate rather than a second row for the same match.
  --
  -- No foreign key on game_id, deliberately: the record has to outlive the room it was played
  -- in, and a FK would invite an ON DELETE CASCADE that took the history with it.
  UNIQUE KEY `ux_twsny_card_game_record_deal` (`game_id`, `started_date`),
  KEY `idx_twsny_card_game_record_game` (`game_id`),
  KEY `idx_twsny_card_game_record_host` (`host_username`),
  KEY `idx_twsny_card_game_record_ended` (`ended_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_card_game_record` AUTO_INCREMENT = 1000;

CREATE TABLE IF NOT EXISTS `twsny_card_game_record_player` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL,
  `username` varchar(100) NOT NULL,
  `seat` int NOT NULL,
  `score` int NOT NULL DEFAULT 0,
  `cpu` bit(1) NOT NULL DEFAULT b'0',
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_twsny_card_game_record_player_record` (`record_id`),
  KEY `idx_twsny_card_game_record_player_user` (`username`),
  -- Here a cascade is right: these rows are part of the record above, not history of their own.
  CONSTRAINT `fk_twsny_card_game_record_player_record`
    FOREIGN KEY (`record_id`) REFERENCES `twsny_card_game_record` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_card_game_record_player` AUTO_INCREMENT = 1000;
