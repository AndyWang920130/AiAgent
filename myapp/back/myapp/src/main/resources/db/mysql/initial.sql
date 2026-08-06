SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `twsny_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `login` varchar(512) NOT NULL,
  `real_name` varchar(512) DEFAULT NULL,
  `nick_name` varchar(512) DEFAULT NULL,
  `password` varchar(512) DEFAULT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `user_type` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  `phone_number` varchar(32) DEFAULT NULL,
  `email` varchar(512) DEFAULT NULL,
  `avatar` varchar(512) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  `deleted` bit(1) DEFAULT b'0',
  `delete_reason` varchar(100) DEFAULT NULL,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_twsny_user_login` (`login`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `twsny_blog` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(500) NOT NULL,
  `content` longtext NOT NULL,
  `summary` varchar(500) DEFAULT NULL,
  `cover_image` varchar(512) DEFAULT NULL,
  `author` varchar(100) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `visibility` varchar(255) DEFAULT 'PUBLIC',
  `view_count` bigint DEFAULT NULL,
  `likes` bigint DEFAULT NULL,
  `comment_count` bigint DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `tag` varchar(50) DEFAULT NULL,
  `tag_color` varchar(50) DEFAULT NULL,
  `deleted` bit(1) DEFAULT b'0',
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_twsny_blog_status` (`status`),
  KEY `idx_twsny_blog_visibility` (`visibility`),
  KEY `idx_twsny_blog_category` (`category`),
  KEY `idx_twsny_blog_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `twsny_blog_config` (
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
  UNIQUE KEY `uk_twsny_blog_config_type_name` (`type`, `name`),
  KEY `idx_twsny_blog_config_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `twsny_blog_view_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL,
  `blog_id` bigint NOT NULL,
  `view_count` bigint NOT NULL DEFAULT 0,
  `first_viewed_date` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `last_viewed_date` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_twsny_blog_view_history_user_blog` (`username`, `blog_id`),
  KEY `idx_twsny_blog_view_history_username` (`username`),
  KEY `idx_twsny_blog_view_history_blog_id` (`blog_id`),
  KEY `idx_twsny_blog_view_history_last_viewed_date` (`last_viewed_date`),
  CONSTRAINT `fk_twsny_blog_view_history_blog_id` FOREIGN KEY (`blog_id`) REFERENCES `twsny_blog` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `twsny_blog_like_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL,
  `blog_id` bigint NOT NULL,
  `liked_date` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_twsny_blog_like_history_user_blog` (`username`, `blog_id`),
  KEY `idx_twsny_blog_like_history_username` (`username`),
  KEY `idx_twsny_blog_like_history_blog_id` (`blog_id`),
  KEY `idx_twsny_blog_like_history_liked_date` (`liked_date`),
  CONSTRAINT `fk_twsny_blog_like_history_blog_id` FOREIGN KEY (`blog_id`) REFERENCES `twsny_blog` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `twsny_blog_comment` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `username` varchar(100) NOT NULL,
    `blog_id` bigint NOT NULL,
    `content` text NOT NULL,
    `created_by` varchar(50) NOT NULL DEFAULT 'system',
    `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
    `last_modified_by` varchar(50) DEFAULT NULL,
    `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    KEY `idx_twsny_blog_comment_blog_id` (`blog_id`),
    KEY `idx_twsny_blog_comment_created_date` (`created_date`),
    CONSTRAINT `fk_twsny_blog_comment_blog_id`
    FOREIGN KEY (`blog_id`) REFERENCES `twsny_blog` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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


INSERT IGNORE INTO `twsny_user`
  (`id`, `login`, `real_name`, `nick_name`, `password`, `email`, `role`, `deleted`, `created_by`, `created_date`, `last_modified_by`, `last_modified_date`)
VALUES
  (1, 'admin', 'Administrator', 'Administrator', '$2a$10$KtddfykXOj0CzPYJEL6cDuqPVbWN5HUBSnyRgy51UFs1AxBdjbfGG', 'admin@example.com', 'ADMIN', b'0', 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (2, 'user', 'Demo User', 'Demo User', '$2a$10$OSrgGE7b8DrxitBTAF3hRetlUyld/mldHYDCTMfS1mjakmcF8H5FG', 'user@example.com', 'USER', b'0', 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6));

INSERT IGNORE INTO `twsny_blog`
  (`id`, `title`, `summary`, `content`, `category`, `tag`, `tag_color`, `author`, `status`, `visibility`, `view_count`, `likes`, `comment_count`, `deleted`, `created_by`, `created_date`, `last_modified_by`, `last_modified_date`)
VALUES
  (1, 'Getting Started with Vue 3 Composition API', 'The Composition API is one of the most exciting features in Vue 3. It provides a flexible way to organize component logic...', '<p>The Composition API is one of the most exciting features in Vue 3. It provides a flexible way to organize component logic.</p><p>By separating concerns into composable functions, you can create more maintainable and reusable code. Key benefits include better TypeScript support, improved code organization, and easier testing.</p>', 'Frontend', 'Vue', 'green', 'admin', 'PUBLISHED', 'PUBLIC', 1240, 89, 23, b'0', 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (2, 'Building Scalable APIs with Spring Boot', 'Spring Boot makes it easy to create stand-alone, production-grade Spring applications with minimal configuration...', '<p>Spring Boot makes it easy to create stand-alone, production-grade Spring applications with minimal configuration.</p><p>With embedded servers and auto-configuration, you can get a REST API running in minutes. The framework handles dependency injection, transaction management, and security out of the box.</p>', 'Backend', 'Java', 'orange', 'admin', 'PUBLISHED', 'PUBLIC', 980, 64, 15, b'0', 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (3, 'TypeScript Best Practices in 2025', 'TypeScript has become the go-to choice for large-scale JavaScript applications. Here are the patterns that matter most...', '<p>TypeScript has become the go-to choice for large-scale JavaScript applications. Here are the patterns that matter most in 2025.</p><ul><li>Always use strict mode</li><li>Prefer type inference over explicit annotations</li><li>Use utility types like Partial, Required, and Pick</li><li>Leverage template literal types for string manipulation</li></ul>', 'Language', 'TypeScript', 'blue', 'admin', 'PUBLISHED', 'PUBLIC', 2100, 142, 37, b'0', 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (4, 'Docker & Kubernetes: A Practical Guide', 'Containerization has revolutionized how we deploy applications. Learn how to effectively use Docker and K8s together...', '<p>Containerization has revolutionized how we deploy applications. Learn how to effectively use Docker and Kubernetes together.</p><p>Start with a solid Dockerfile, define resource limits in your K8s manifests, and use Helm charts to manage complex deployments across environments.</p>', 'DevOps', 'DevOps', 'purple', 'admin', 'PUBLISHED', 'PUBLIC', 1560, 98, 29, b'0', 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6));

INSERT IGNORE INTO `twsny_blog_config`
  (`id`, `type`, `name`, `config_value`, `description`, `sort_order`, `created_by`, `created_date`, `last_modified_by`, `last_modified_date`)
VALUES
  (1, 'CATEGORY', 'Frontend', NULL, 'UI, Vue, JavaScript, CSS', 10, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (2, 'CATEGORY', 'Backend', NULL, 'Services, APIs, databases', 20, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (3, 'CATEGORY', 'Language', NULL, 'Programming language notes', 30, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (4, 'CATEGORY', 'DevOps', NULL, 'Build, deployment, operations', 40, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (5, 'TAG', 'Vue', NULL, NULL, 10, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (6, 'TAG', 'Java', NULL, NULL, 20, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (7, 'TAG', 'Spring', NULL, NULL, 30, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (8, 'TAG', 'TypeScript', NULL, NULL, 40, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (9, 'TAG_COLOR', 'Blue', 'blue', NULL, 10, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (10, 'TAG_COLOR', 'Green', 'green', NULL, 20, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (11, 'TAG_COLOR', 'Orange', 'orange', NULL, 30, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (12, 'TAG_COLOR', 'Purple', 'purple', NULL, 40, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (13, 'TAG_COLOR', 'Red', 'red', NULL, 50, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (14, 'TAG_COLOR', 'Cyan', 'cyan', NULL, 60, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6)),
  (15, 'TAG_COLOR', 'Geek Blue', 'geekblue', NULL, 70, 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6));

ALTER TABLE `twsny_user` AUTO_INCREMENT = 1000;
ALTER TABLE `twsny_blog` AUTO_INCREMENT = 1000;
ALTER TABLE `twsny_blog_config` AUTO_INCREMENT = 1000;
ALTER TABLE `twsny_blog_view_history` AUTO_INCREMENT = 1000;
ALTER TABLE `twsny_blog_like_history` AUTO_INCREMENT = 1000;
ALTER TABLE `twsny_achievement` AUTO_INCREMENT = 1000;
ALTER TABLE `twsny_user_follow` AUTO_INCREMENT = 1000;
ALTER TABLE `twsny_gomoku_game` AUTO_INCREMENT = 1000;
ALTER TABLE `twsny_notification` AUTO_INCREMENT = 1000;

SET FOREIGN_KEY_CHECKS = 1;
