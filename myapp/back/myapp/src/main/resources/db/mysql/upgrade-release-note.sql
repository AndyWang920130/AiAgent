-- Upgrade: the About section's release notes.
--
-- Adds twsny_release_note, one row per release, backing /about/release-notes. Administrators
-- write and publish entries there; everybody else reads the published ones.
-- Safe to re-run (CREATE TABLE IF NOT EXISTS). Prod runs with ddl-auto=none, so apply manually.
--
-- No seed rows. The page shows an empty state until the first note is written, and inventing
-- history for releases that shipped before this table existed would put dates in it that
-- nobody can vouch for.

CREATE TABLE IF NOT EXISTS `twsny_release_note` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` varchar(50) NOT NULL,
  `title` varchar(200) DEFAULT NULL,
  `content` longtext NOT NULL,
  -- When the release happened, which is what the page is ordered by. Distinct from
  -- created_date below: that is when somebody typed the note.
  `released_date` datetime(6) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `created_by` varchar(50) NOT NULL DEFAULT 'system',
  `created_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `last_modified_by` varchar(50) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  -- One note per release. A second row for a version already covered is a mistake, and it is
  -- better as a failed save than as a page listing the same release twice.
  UNIQUE KEY `ux_twsny_release_note_version` (`version`),
  -- The public list is "published, newest release first" — both columns, in that order.
  KEY `idx_twsny_release_note_status_released` (`status`, `released_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `twsny_release_note` AUTO_INCREMENT = 1000;
