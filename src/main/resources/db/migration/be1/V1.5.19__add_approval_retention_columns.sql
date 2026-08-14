ALTER TABLE `approval_document`
  ADD COLUMN `retention_policy` varchar(30) NULL AFTER `resubmitted_at`,
  ADD COLUMN `retention_until` datetime(6) NULL AFTER `retention_policy`,
  ADD COLUMN `legal_hold` tinyint(1) NOT NULL DEFAULT 0 AFTER `retention_until`,
  ADD COLUMN `archived_at` datetime(6) NULL AFTER `legal_hold`;

UPDATE `approval_document`
SET `retention_policy` = 'GENERAL_BUSINESS',
    `retention_until` = DATE_ADD(`created_at`, INTERVAL 3 YEAR)
WHERE `retention_policy` IS NULL;

SET @approval_source_type_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`columns`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'approval_document'
    AND `column_name` = 'source_type'
);

SET @approval_retention_update_sql = IF(
  @approval_source_type_exists > 0,
  'UPDATE `approval_document`
   SET `retention_policy` = CASE
       WHEN `source_type` = ''CORPORATE_CARD_EXPENSE'' THEN ''TAX_EVIDENCE''
       ELSE `retention_policy`
     END,
     `retention_until` = CASE
       WHEN `source_type` = ''CORPORATE_CARD_EXPENSE'' THEN DATE_ADD(`created_at`, INTERVAL 5 YEAR)
       ELSE `retention_until`
     END',
  'SELECT 1'
);

PREPARE approval_retention_update_stmt FROM @approval_retention_update_sql;
EXECUTE approval_retention_update_stmt;
DEALLOCATE PREPARE approval_retention_update_stmt;

ALTER TABLE `approval_document`
  MODIFY COLUMN `retention_policy` varchar(30) NOT NULL,
  MODIFY COLUMN `retention_until` datetime(6) NOT NULL;

CREATE INDEX `idx_approval_document_retention_until`
  ON `approval_document` (`retention_until`, `approval_document_id`);

CREATE INDEX `idx_approval_document_archived_at`
  ON `approval_document` (`archived_at`, `approval_document_id`);
