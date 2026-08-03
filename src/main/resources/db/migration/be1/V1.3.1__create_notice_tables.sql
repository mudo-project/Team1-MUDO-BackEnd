CREATE TABLE `notice` (
    `notice_id`       BIGINT       NOT NULL AUTO_INCREMENT,
    `academy_id`      BIGINT       NOT NULL,
    `author_user_id`  BIGINT       NOT NULL,
    `title`           VARCHAR(200) NOT NULL,
    `content`         TEXT         NOT NULL,
    `is_pinned`       TINYINT(1)   NOT NULL DEFAULT 0,
    `view_count`      BIGINT       NOT NULL DEFAULT 0,
    `created_at`      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notice_attachment` (
    `attachment_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `notice_id`     BIGINT       NOT NULL,
    `file_url`      VARCHAR(500) NOT NULL,
    `file_name`     VARCHAR(200) NOT NULL,
    `file_type`     VARCHAR(50)  NULL,
    PRIMARY KEY (`attachment_id`),
    CONSTRAINT `fk_notice_attachment_notice` FOREIGN KEY (`notice_id`)
        REFERENCES `notice` (`notice_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
