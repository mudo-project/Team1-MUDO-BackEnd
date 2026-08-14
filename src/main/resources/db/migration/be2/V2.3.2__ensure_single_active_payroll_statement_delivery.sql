ALTER TABLE payroll_statement_delivery
    DROP CHECK chk_payroll_delivery_status,
    ADD COLUMN attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at DATETIME NULL,
    ADD COLUMN last_attempt_at DATETIME NULL,
    ADD COLUMN last_reconciled_at DATETIME NULL,
    ADD COLUMN active_statement_id BIGINT GENERATED ALWAYS AS (
        CASE
            WHEN status IN (
                'PENDING', 'SENDING', 'RETRY_WAIT', 'UNKNOWN', 'SENT', 'DELIVERED'
            ) THEN statement_id
            ELSE NULL
        END
    ) STORED,
    ADD CONSTRAINT uk_payroll_delivery_active_statement UNIQUE (active_statement_id),
    ADD CONSTRAINT chk_payroll_delivery_status
        CHECK (status IN (
            'PENDING', 'SENDING', 'RETRY_WAIT', 'UNKNOWN',
            'SENT', 'DELIVERED', 'FAILED', 'SKIPPED'
        )),
    ADD INDEX idx_payroll_delivery_dispatch (status, next_attempt_at, delivery_id),
    ADD INDEX idx_payroll_delivery_reconcile (status, last_reconciled_at, delivery_id);
