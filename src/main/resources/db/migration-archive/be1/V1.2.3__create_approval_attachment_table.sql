CREATE TABLE `approval_attachment` (
    `id`                    BIGINT      NOT NULL AUTO_INCREMENT,
    `approval_document_id`  BIGINT      NOT NULL,
    `file_id`               BIGINT      NOT NULL,
    `ai_summary`            TEXT        NULL,
    `summary_status`        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `summarized_at`         DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_approval_attachment_document_file` (`approval_document_id`, `file_id`),
    CONSTRAINT `fk_approval_attachment_document` FOREIGN KEY (`approval_document_id`)
        REFERENCES `approval_document` (`approval_document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
