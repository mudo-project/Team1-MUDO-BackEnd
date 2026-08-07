CREATE TABLE `notice_read` (
    `id`        BIGINT      NOT NULL AUTO_INCREMENT,
    `notice_id` BIGINT      NOT NULL,
    `user_id`   BIGINT      NOT NULL,
    `read_at`   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_read_notice_user` (`notice_id`, `user_id`),
    CONSTRAINT `fk_notice_read_notice` FOREIGN KEY (`notice_id`)
        REFERENCES `notice` (`notice_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
