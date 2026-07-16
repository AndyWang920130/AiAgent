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
  CONSTRAINT `fk_twsny_blog_like_history_blog_id`
    FOREIGN KEY (`blog_id`) REFERENCES `twsny_blog` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
