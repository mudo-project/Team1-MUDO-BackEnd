ALTER TABLE `file_metadata`
  ADD COLUMN `academy_id` BIGINT NOT NULL AFTER `id`,
  ADD INDEX `idx_file_metadata_academy_id` (`academy_id`);
