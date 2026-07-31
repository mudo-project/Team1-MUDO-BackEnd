CREATE TABLE `users` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(50)  NOT NULL,
    `role`         VARCHAR(20)  NOT NULL,
    `phone_number` VARCHAR(20)  NOT NULL,
    `email`        VARCHAR(100) NOT NULL,
    `username`     VARCHAR(50)  NOT NULL,
    `hire_date`    DATE         NOT NULL,
    `resign_date`  DATE         NULL,
    `academy_id`   BIGINT       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
