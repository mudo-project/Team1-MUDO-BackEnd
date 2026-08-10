ALTER TABLE google_account_connection
    CHANGE COLUMN token_expires_at refresh_token_expires_at DATETIME(6) NULL;

UPDATE google_account_connection
SET refresh_token_expires_at = NULL;
