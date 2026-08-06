ALTER TABLE `chat_task_card`
    ADD COLUMN `deleted_at` DATETIME(6) NULL AFTER `created_at`;

CREATE INDEX `idx_chat_task_card_room_deleted`
    ON `chat_task_card` (`chat_room_id`, `deleted_at`, `created_at`);
