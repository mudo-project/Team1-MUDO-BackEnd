CREATE TABLE `chat_task_card` (
    `card_id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `chat_room_id`     BIGINT       NOT NULL,
    `assigner_user_id` BIGINT       NOT NULL,
    `content`          TEXT         NOT NULL,
    `due_date`         DATE         NULL,
    `created_at`       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`card_id`),
    CONSTRAINT `fk_chat_task_card_room` FOREIGN KEY (`chat_room_id`)
        REFERENCES `chat_room` (`chat_room_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_task_assignee` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT,
    `card_id`      BIGINT      NOT NULL,
    `user_id`      BIGINT      NOT NULL,
    `completed_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_task_assignee_card_user` (`card_id`, `user_id`),
    CONSTRAINT `fk_chat_task_assignee_card` FOREIGN KEY (`card_id`)
        REFERENCES `chat_task_card` (`card_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_chat_task_card_room` ON `chat_task_card` (`chat_room_id`);
