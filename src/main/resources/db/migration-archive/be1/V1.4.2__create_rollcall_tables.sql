CREATE TABLE `attendance_entry` (
    `entry_id`    BIGINT       NOT NULL AUTO_INCREMENT,
    `academy_id`  BIGINT       NOT NULL,
    `lecture_id`  BIGINT       NOT NULL,
    `student_id`  BIGINT       NOT NULL,
    `entry_date`  DATE         NOT NULL,
    `status`      VARCHAR(20)  NOT NULL,
    `note`        VARCHAR(500) NULL,
    `created_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`entry_id`),
    UNIQUE KEY `uk_attendance_entry_lecture_student_date` (`lecture_id`, `student_id`, `entry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `message_template` (
    `template_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `academy_id`  BIGINT       NOT NULL,
    `name`        VARCHAR(100) NOT NULL,
    `status`      VARCHAR(20)  NOT NULL,
    `content`     VARCHAR(1000) NOT NULL,
    `created_by`  BIGINT       NOT NULL,
    `created_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`template_id`),
    UNIQUE KEY `uk_message_template_academy_status` (`academy_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
