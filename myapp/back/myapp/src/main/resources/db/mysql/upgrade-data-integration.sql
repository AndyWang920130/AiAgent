SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- Data Integration schema. Idempotent: safe to run on a fresh database (creates
-- the table) or on an existing one that is missing some of the newer columns
-- (adds only what's absent). Prod runs with spring.jpa.hibernate.ddl-auto=none,
-- so this script is the source of truth for the table there — run it after any
-- deploy that adds columns.
-- ---------------------------------------------------------------------------

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

-- ---------------------------------------------------------------------------
-- Add-if-missing for columns introduced after the table first shipped. MySQL 8
-- has no `ADD COLUMN IF NOT EXISTS`, so each add is guarded against
-- information_schema and only runs when the column is actually absent. This makes
-- the whole script re-runnable without "duplicate column" errors.
-- ---------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS `twsny_di_add_column`;
DELIMITER $$
CREATE PROCEDURE `twsny_di_add_column`(IN col_name VARCHAR(64), IN col_ddl VARCHAR(255))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'twsny_data_integration'
      AND COLUMN_NAME = col_name
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `twsny_data_integration` ADD COLUMN ', col_ddl);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

-- Auth-chaining columns (first data-integration release).
CALL `twsny_di_add_column`('auth_source_id',      '`auth_source_id` bigint DEFAULT NULL');
CALL `twsny_di_add_column`('auth_token_path',      '`auth_token_path` varchar(500) DEFAULT NULL');
CALL `twsny_di_add_column`('auth_header_name',     '`auth_header_name` varchar(100) DEFAULT NULL');
CALL `twsny_di_add_column`('auth_header_template', '`auth_header_template` varchar(500) DEFAULT NULL');
-- Raw JSON body support.
CALL `twsny_di_add_column`('body_type',            '`body_type` varchar(10) DEFAULT NULL');
CALL `twsny_di_add_column`('body_raw',             '`body_raw` longtext');
-- Body-property injection from the preceding step.
CALL `twsny_di_add_column`('auth_body_property',   '`auth_body_property` varchar(500) DEFAULT NULL');

DROP PROCEDURE IF EXISTS `twsny_di_add_column`;
