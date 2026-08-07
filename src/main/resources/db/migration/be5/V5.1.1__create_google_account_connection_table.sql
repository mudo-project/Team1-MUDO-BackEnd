CREATE TABLE google_account_connection (
    connection_id BIGINT NOT NULL AUTO_INCREMENT,
    academy_id BIGINT NOT NULL,
    google_email VARCHAR(255) NOT NULL,
    connected_by_user_id BIGINT NOT NULL,
    scope VARCHAR(500) NOT NULL,
    encrypted_refresh_token VARCHAR(2048) NOT NULL,
    connected_at DATETIME(6) NOT NULL,
    token_expires_at DATETIME(6) NOT NULL,
    last_checked_at DATETIME(6) NOT NULL,
    failed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_google_account_connection PRIMARY KEY (connection_id),
    CONSTRAINT uk_google_account_connection_academy UNIQUE (academy_id),
    CONSTRAINT fk_google_account_connection_academy
        FOREIGN KEY (academy_id) REFERENCES academy(academy_id),
    CONSTRAINT fk_google_account_connection_connected_by
        FOREIGN KEY (connected_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
