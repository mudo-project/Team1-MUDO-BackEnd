ALTER TABLE `lecture`
  ADD COLUMN `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER `created_at`,
  ADD COLUMN `deleted_at` DATETIME(6) NULL AFTER `updated_at`;

ALTER TABLE `lecture`
  ADD INDEX `idx_lecture_deleted_at` (`deleted_at`);
