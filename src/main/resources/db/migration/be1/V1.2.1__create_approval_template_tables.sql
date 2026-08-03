CREATE TABLE `template` (
    `template_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `academy_id`  BIGINT       NOT NULL,
    `file_id`     BIGINT       NULL,
    `type`        VARCHAR(30)  NOT NULL,
    `name`        VARCHAR(100) NOT NULL,
    `created_by`  BIGINT       NOT NULL,
    `created_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `approval_line_step` (
    `route_step_id` BIGINT NOT NULL AUTO_INCREMENT,
    `template_id`   BIGINT NOT NULL,
    `step_order`    INT    NOT NULL,
    `role_id`       BIGINT NULL,
    `approver_id`   BIGINT NULL,
    PRIMARY KEY (`route_step_id`),
    UNIQUE KEY `uk_approval_line_step_template_step` (`template_id`, `step_order`),
    CONSTRAINT `fk_approval_line_step_template` FOREIGN KEY (`template_id`)
        REFERENCES `template` (`template_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
