CREATE TABLE IF NOT EXISTS `approval_history_hidden` (
  `approval_history_hidden_id` bigint NOT NULL AUTO_INCREMENT,
  `approval_document_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `hidden_at` datetime(6) NOT NULL,
  PRIMARY KEY (`approval_history_hidden_id`),
  UNIQUE KEY `uk_approval_history_hidden_document_user` (`approval_document_id`, `user_id`),
  KEY `idx_approval_history_hidden_user` (`user_id`),
  CONSTRAINT `fk_approval_history_hidden_document`
    FOREIGN KEY (`approval_document_id`) REFERENCES `approval_document` (`approval_document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `permission` (`code`, `resource`, `action`, `description`)
SELECT 'APPROVAL:READ_ALL', 'APPROVAL', 'READ_ALL', '소속 학원 전체 결재 문서 조회'
WHERE NOT EXISTS (
  SELECT 1 FROM `permission` WHERE `code` = 'APPROVAL:READ_ALL'
);
