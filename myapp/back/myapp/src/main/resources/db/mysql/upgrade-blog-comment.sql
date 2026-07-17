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
