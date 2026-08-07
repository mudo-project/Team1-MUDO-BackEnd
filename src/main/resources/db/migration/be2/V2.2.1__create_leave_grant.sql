CREATE TABLE leave_grant (
    leave_grant_id BIGINT NOT NULL AUTO_INCREMENT,
    academy_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    grant_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    granted_days SMALLINT UNSIGNED NOT NULL DEFAULT 15,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_leave_grant
        PRIMARY KEY (leave_grant_id),

    CONSTRAINT fk_leave_grant_academy
        FOREIGN KEY (academy_id)
        REFERENCES academy (academy_id),

    CONSTRAINT fk_leave_grant_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT uk_leave_grant_user_date
        UNIQUE (academy_id, user_id, grant_date),

    CONSTRAINT chk_leave_grant_days
        CHECK (granted_days > 0),

    CONSTRAINT chk_leave_grant_expiration
        CHECK (expiration_date >= grant_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_leave_grant_user_expiration
    ON leave_grant (academy_id, user_id, expiration_date);

ALTER TABLE leave_request
    ADD COLUMN used_days SMALLINT UNSIGNED NOT NULL AFTER end_date,
    ADD CONSTRAINT chk_leave_request_used_days
        CHECK (used_days > 0);

CREATE INDEX idx_leave_request_user_status_date
    ON leave_request (academy_id, user_id, status, start_date, end_date);
