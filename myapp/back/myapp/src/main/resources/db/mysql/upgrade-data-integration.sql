SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `twsny_data_integration` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `base_url` varchar(1000) NOT NULL,
  `path` varchar(1000) DEFAULT NULL,
  `method` varchar(10) DEFAULT NULL,
  `headers` longtext,
  `query_params` longtext,
  `body_config` longtext,
  `body_type` varchar(10) DEFAULT NULL,
  `body_raw` longtext,
  `response_config` longtext,
  `auth_source_id` bigint DEFAULT NULL,
  `auth_token_path` varchar(500) DEFAULT NULL,
  `auth_header_name` varchar(100) DEFAULT NULL,
  `auth_header_template` varchar(500) DEFAULT NULL,
  `auth_body_property` varchar(500) DEFAULT NULL,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_data_integration` AUTO_INCREMENT = 1000;

-- Auth-chaining columns (for databases created before this feature). Safe to skip
-- if the CREATE TABLE above already added them; run individually as needed.
-- ALTER TABLE `twsny_data_integration` ADD COLUMN `auth_source_id` bigint DEFAULT NULL;
-- ALTER TABLE `twsny_data_integration` ADD COLUMN `auth_token_path` varchar(500) DEFAULT NULL;
-- ALTER TABLE `twsny_data_integration` ADD COLUMN `auth_header_name` varchar(100) DEFAULT NULL;
-- ALTER TABLE `twsny_data_integration` ADD COLUMN `auth_header_template` varchar(500) DEFAULT NULL;
-- ALTER TABLE `twsny_data_integration` ADD COLUMN `auth_body_property` varchar(500) DEFAULT NULL;

-- Raw-body columns (for databases created before raw JSON body support). Run as needed.
-- ALTER TABLE `twsny_data_integration` ADD COLUMN `body_type` varchar(10) DEFAULT NULL;
-- ALTER TABLE `twsny_data_integration` ADD COLUMN `body_raw` longtext;
