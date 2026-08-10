CREATE TABLE `term` (
    `term_id`    BIGINT      NOT NULL AUTO_INCREMENT,
    `academy_id` BIGINT      NOT NULL,
    `name`       VARCHAR(100) NOT NULL,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`term_id`),
    UNIQUE KEY `uk_term_academy_name` (`academy_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `subject` (
    `subject_id` BIGINT      NOT NULL AUTO_INCREMENT,
    `academy_id` BIGINT      NOT NULL,
    `name`       VARCHAR(100) NOT NULL,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`subject_id`),
    UNIQUE KEY `uk_subject_academy_name` (`academy_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `classroom` (
    `classroom_id` BIGINT      NOT NULL AUTO_INCREMENT,
    `academy_id`   BIGINT      NOT NULL,
    `name`         VARCHAR(100) NOT NULL,
    `created_at`   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`classroom_id`),
    UNIQUE KEY `uk_classroom_academy_name` (`academy_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `lecture` (
    `lecture_id`   BIGINT       NOT NULL AUTO_INCREMENT,
    `academy_id`   BIGINT       NOT NULL,
    `name`         VARCHAR(100) NOT NULL,
    `grade`        VARCHAR(20)  NOT NULL,
    `term_id`      BIGINT       NOT NULL,
    `subject_id`   BIGINT       NOT NULL,
    `teacher_id`   BIGINT       NOT NULL,
    `classroom_id` BIGINT       NOT NULL,
    `fee_type`     VARCHAR(20)  NULL,
    `fee_amount`   INT          NULL,
    `created_at`   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`lecture_id`),
    CONSTRAINT `fk_lecture_term` FOREIGN KEY (`term_id`) REFERENCES `term` (`term_id`),
    CONSTRAINT `fk_lecture_subject` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`subject_id`),
    CONSTRAINT `fk_lecture_classroom` FOREIGN KEY (`classroom_id`) REFERENCES `classroom` (`classroom_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `lecture_schedule` (
    `schedule_id`  BIGINT      NOT NULL AUTO_INCREMENT,
    `lecture_id`   BIGINT      NOT NULL,
    `day_of_week`  VARCHAR(10) NOT NULL,
    `start_time`   TIME        NOT NULL,
    `end_time`     TIME        NOT NULL,
    PRIMARY KEY (`schedule_id`),
    KEY `idx_lecture_schedule_classroom_lookup` (`lecture_id`),
    CONSTRAINT `fk_lecture_schedule_lecture` FOREIGN KEY (`lecture_id`)
        REFERENCES `lecture` (`lecture_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `student` (
    `student_id`   BIGINT      NOT NULL AUTO_INCREMENT,
    `academy_id`   BIGINT      NOT NULL,
    `name`         VARCHAR(50) NOT NULL,
    `grade`        VARCHAR(20) NOT NULL,
    `school`       VARCHAR(100) NULL,
    `phone`        VARCHAR(20) NULL,
    `parent_phone` VARCHAR(20) NULL,
    `note`         VARCHAR(500) NULL,
    `created_at`   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `enrollment` (
    `enrollment_id` BIGINT      NOT NULL AUTO_INCREMENT,
    `student_id`    BIGINT      NOT NULL,
    `lecture_id`    BIGINT      NOT NULL,
    `created_at`    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`enrollment_id`),
    UNIQUE KEY `uk_enrollment_student_lecture` (`student_id`, `lecture_id`),
    CONSTRAINT `fk_enrollment_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
    CONSTRAINT `fk_enrollment_lecture` FOREIGN KEY (`lecture_id`) REFERENCES `lecture` (`lecture_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
