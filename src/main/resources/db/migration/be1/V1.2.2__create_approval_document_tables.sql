CREATE TABLE `approval_document` (
    `approval_document_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `academy_id`           BIGINT       NOT NULL,
    `template_id`          BIGINT       NOT NULL,
    `requester_user_id`    BIGINT       NOT NULL,
    `title`                VARCHAR(200) NOT NULL,
    `content_type`         VARCHAR(20)  NOT NULL,
    `text`                 TEXT         NULL,
    `status`               VARCHAR(20)  NOT NULL,
    `created_at`           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `resubmitted_at`       DATETIME(6)  NULL,
    PRIMARY KEY (`approval_document_id`),
    CONSTRAINT `fk_approval_document_template` FOREIGN KEY (`template_id`, `academy_id`)
        REFERENCES `template` (`template_id`, `academy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `approval_step` (
    `document_step_id`     BIGINT        NOT NULL AUTO_INCREMENT,
    `approval_document_id` BIGINT        NOT NULL,
    `step_order`           INT           NOT NULL,
    `approver_user_id`     BIGINT        NOT NULL,
    `status`                VARCHAR(20)  NOT NULL,
    `comment`               VARCHAR(1000) NULL,
    `decided_at`            DATETIME(6)  NULL,
    PRIMARY KEY (`document_step_id`),
    UNIQUE KEY `uk_approval_step_document_step` (`approval_document_id`, `step_order`),
    CONSTRAINT `fk_approval_step_document` FOREIGN KEY (`approval_document_id`)
        REFERENCES `approval_document` (`approval_document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
