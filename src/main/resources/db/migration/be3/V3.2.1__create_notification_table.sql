CREATE TABLE notification (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    message VARCHAR(250) NOT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    CONSTRAINT pk_notification PRIMARY KEY (notification_id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(id),
    KEY idx_notification_recipient_deleted_created (recipient_user_id, deleted_at, created_at),
    KEY idx_notification_recipient_deleted_read (recipient_user_id, deleted_at, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
