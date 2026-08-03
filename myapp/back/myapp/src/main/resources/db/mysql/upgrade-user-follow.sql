-- Upgrade: user follow relationships.
-- Adds the twsny_user_follow table backing the "follow a user" feature. Safe to re-run
-- (CREATE TABLE IF NOT EXISTS). Prod runs with spring.jpa.hibernate.ddl-auto=none, so this
-- must be applied manually / by the deploy pipeline.

CREATE TABLE IF NOT EXISTS `twsny_user_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `follower_username` varchar(100) NOT NULL,
  `following_username` varchar(100) NOT NULL,
  `followed_date` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_twsny_user_follow_follower_following` (`follower_username`, `following_username`),
  KEY `idx_twsny_user_follow_follower` (`follower_username`),
  KEY `idx_twsny_user_follow_following` (`following_username`),
  KEY `idx_twsny_user_follow_followed_date` (`followed_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_user_follow` AUTO_INCREMENT = 1000;
