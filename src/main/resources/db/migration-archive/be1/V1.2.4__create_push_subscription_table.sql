CREATE TABLE `push_subscription` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `endpoint`   VARCHAR(500) NOT NULL,
    `p256dh`     VARCHAR(255) NOT NULL,
    `auth`       VARCHAR(255) NOT NULL,
    `created_at` DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_push_subscription_user_endpoint` (`user_id`, `endpoint`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
