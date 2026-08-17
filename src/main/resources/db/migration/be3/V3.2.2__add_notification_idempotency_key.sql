ALTER TABLE notification
    ADD COLUMN idempotency_key VARCHAR(255) NOT NULL DEFAULT '' AFTER message;

UPDATE notification
SET idempotency_key = CONCAT(type, ':', target_id, ':', recipient_user_id, ':', notification_id)
WHERE idempotency_key = '';

ALTER TABLE notification
    ALTER COLUMN idempotency_key DROP DEFAULT;

ALTER TABLE notification
    ADD CONSTRAINT uk_notification_idempotency_key UNIQUE (idempotency_key);
