ALTER TABLE users
    ADD COLUMN status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER role,
    ADD COLUMN joined_at          DATETIME    NULL,
    ADD COLUMN must_change_pw     BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN is_platform_admin  BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

UPDATE users SET joined_at = hire_date;
UPDATE users SET status = 'RESIGNED' WHERE resign_date IS NOT NULL;

ALTER TABLE users
    DROP COLUMN hire_date,
    DROP COLUMN resign_date;
