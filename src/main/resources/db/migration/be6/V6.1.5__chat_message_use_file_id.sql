ALTER TABLE `chat_message`
  ADD COLUMN `file_id` BIGINT NULL AFTER `chat_room_id`,
  ADD CONSTRAINT `fk_chat_message_file` FOREIGN KEY (`file_id`) REFERENCES `file_metadata` (`id`),
  DROP COLUMN `file_url`;
