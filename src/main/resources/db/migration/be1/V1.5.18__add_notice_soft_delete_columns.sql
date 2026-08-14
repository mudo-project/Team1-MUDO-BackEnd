ALTER TABLE `notice`
  ADD COLUMN `deleted_at` DATETIME(6) NULL AFTER `updated_at`,
  ADD COLUMN `retention_until` DATETIME(6) NULL AFTER `deleted_at`;

CREATE INDEX `idx_notice_active_pinned_created`
  ON `notice` (`deleted_at`, `is_pinned`, `created_at`, `notice_id`);

CREATE INDEX `idx_notice_retention_until`
  ON `notice` (`retention_until`, `notice_id`);
