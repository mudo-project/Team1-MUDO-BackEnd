ALTER TABLE `chat_message`
    ADD COLUMN `edited_at` DATETIME(6) NULL AFTER `created_at`,
    ADD COLUMN `deleted_at` DATETIME(6) NULL AFTER `edited_at`;

CREATE INDEX `idx_chat_message_room_deleted_created`
    ON `chat_message` (`chat_room_id`, `deleted_at`, `created_at`);
