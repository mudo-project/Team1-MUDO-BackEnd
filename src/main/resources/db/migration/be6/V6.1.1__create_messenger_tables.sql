CREATE TABLE `chat_room` (
    `chat_room_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `academy_id`   BIGINT       NOT NULL,
    `name`         VARCHAR(100) NULL,
    `type`         VARCHAR(10)  NOT NULL,
    `created_by`   BIGINT       NOT NULL,
    `created_at`   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`chat_room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_room_member` (
    `chat_room_id`  BIGINT      NOT NULL,
    `user_id`       BIGINT      NOT NULL,
    `last_read_at`  DATETIME(6) NULL,
    PRIMARY KEY (`chat_room_id`, `user_id`),
    CONSTRAINT `fk_chat_room_member_room` FOREIGN KEY (`chat_room_id`)
        REFERENCES `chat_room` (`chat_room_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_message` (
    `message_id`      BIGINT       NOT NULL AUTO_INCREMENT,
    `chat_room_id`    BIGINT       NOT NULL,
    `sender_user_id`  BIGINT       NOT NULL,
    `message_type`    VARCHAR(10)  NOT NULL,
    `content`         TEXT         NULL,
    `file_url`        VARCHAR(500) NULL,
    `file_name`       VARCHAR(200) NULL,
    `created_at`      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`message_id`),
    CONSTRAINT `fk_chat_message_room` FOREIGN KEY (`chat_room_id`)
        REFERENCES `chat_room` (`chat_room_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_chat_message_room_created` ON `chat_message` (`chat_room_id`, `created_at`);
