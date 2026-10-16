SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `twsny_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `market` varchar(10) NOT NULL,
  `code` varchar(20) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `currency` varchar(10) DEFAULT NULL,
  `latest_price` decimal(19,4) DEFAULT NULL,
  `previous_close` decimal(19,4) DEFAULT NULL,
  `open_price` decimal(19,4) DEFAULT NULL,
  `day_high` decimal(19,4) DEFAULT NULL,
  `day_low` decimal(19,4) DEFAULT NULL,
  `price_change` decimal(19,4) DEFAULT NULL,
  `change_percent` decimal(19,4) DEFAULT NULL,
  `volume` bigint DEFAULT NULL,
  `last_quote_time` datetime(6) DEFAULT NULL,
  `market_time_text` varchar(50) DEFAULT NULL,
  `last_quote_error` varchar(500) DEFAULT NULL,
  `quote_provider` varchar(20) DEFAULT NULL,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_twsny_stock_market_code` (`market`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_stock` AUTO_INCREMENT = 1000;

-- Per-user watch lists. twsny_stock holds one shared row per symbol so a quote is
-- fetched once and its history stays continuous; this table decides who sees it.
CREATE TABLE IF NOT EXISTS `twsny_stock_watch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `stock_id` bigint NOT NULL,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_twsny_stock_watch_username_stock` (`username`, `stock_id`),
  KEY `idx_twsny_stock_watch_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_stock_watch` AUTO_INCREMENT = 1000;

CREATE TABLE IF NOT EXISTS `twsny_stock_price_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stock_id` bigint NOT NULL,
  `price` decimal(19,4) NOT NULL,
  `price_change` decimal(19,4) DEFAULT NULL,
  `change_percent` decimal(19,4) DEFAULT NULL,
  `volume` bigint DEFAULT NULL,
  `recorded_date` datetime(6) NOT NULL,
  `market_time_text` varchar(50) DEFAULT NULL,
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_twsny_stock_price_history_stock_id` (`stock_id`),
  KEY `idx_twsny_stock_price_history_recorded_date` (`recorded_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_stock_price_history` AUTO_INCREMENT = 1000;
