ALTER TABLE `notice_attachment`
  ADD COLUMN `file_id` BIGINT NOT NULL AFTER `notice_id`,
  ADD CONSTRAINT `fk_notice_attachment_file` FOREIGN KEY (`file_id`) REFERENCES `file_metadata` (`id`),
  DROP COLUMN `file_url`,
  DROP COLUMN `file_type`;
