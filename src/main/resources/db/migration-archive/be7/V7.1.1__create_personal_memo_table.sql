CREATE TABLE `personal_memo` (
    `memo_id`     BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `title`       VARCHAR(100) NOT NULL,
    `content`     TEXT         NULL,
    `color`       VARCHAR(10)  NOT NULL,
    `position_x`  INT          NULL,
    `position_y`  INT          NULL,
    `width`       INT          NULL,
    `height`      INT          NULL,
    `created_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`memo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_personal_memo_user` ON `personal_memo` (`user_id`);
