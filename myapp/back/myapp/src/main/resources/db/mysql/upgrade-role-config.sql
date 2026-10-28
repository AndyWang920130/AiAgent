CREATE TABLE IF NOT EXISTS `twsny_role_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `sort_order` int DEFAULT 0,
  `enabled` bit(1) DEFAULT b'1',
  `built_in` bit(1) DEFAULT b'0',
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_twsny_role_config_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- The two roles the application itself depends on: SecurityConfig names ADMIN directly,
-- and USER is the fallback for an account with no role set. Both are flagged built_in,
-- which stops the Role Config page deleting, disabling or re-coding them.
INSERT IGNORE INTO `twsny_role_config`
  (`id`, `code`, `name`, `description`, `sort_order`, `enabled`, `built_in`, `created_by`)
VALUES
  (1, 'ADMIN', 'Administrator', 'Full access, including system settings and user management', 10, b'1', b'1', 'system'),
  (2, 'USER', 'Regular User', 'Standard access for everyday accounts', 20, b'1', b'1', 'system');

ALTER TABLE `twsny_role_config` AUTO_INCREMENT = 1000;
