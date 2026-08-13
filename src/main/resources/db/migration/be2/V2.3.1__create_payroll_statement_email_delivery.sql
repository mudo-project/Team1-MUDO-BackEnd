CREATE TABLE payroll_statement_delivery_batch (
    batch_id BIGINT NOT NULL AUTO_INCREMENT,
    payroll_year_month DATE NOT NULL,
    requested_by BIGINT NOT NULL,
    requested_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (batch_id),
    CONSTRAINT fk_payroll_delivery_batch_requester
        FOREIGN KEY (requested_by) REFERENCES users (id),
    INDEX idx_payroll_delivery_batch_month (payroll_year_month, batch_id)
);

CREATE TABLE payroll_statement_delivery (
    delivery_id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NULL,
    payroll_id BIGINT NOT NULL,
    statement_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    recipient_email VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL,
    failure_code VARCHAR(100) NULL,
    failure_reason VARCHAR(1000) NULL,
    delivery_token VARCHAR(36) NOT NULL,
    mailgun_message_id VARCHAR(255) NULL,
    requested_by BIGINT NOT NULL,
    requested_at DATETIME NOT NULL,
    sending_started_at DATETIME NULL,
    sent_at DATETIME NULL,
    delivered_at DATETIME NULL,
    failed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (delivery_id),
    CONSTRAINT fk_payroll_delivery_batch
        FOREIGN KEY (batch_id) REFERENCES payroll_statement_delivery_batch (batch_id),
    CONSTRAINT fk_payroll_delivery_payroll
        FOREIGN KEY (payroll_id) REFERENCES payroll (payroll_id),
    CONSTRAINT fk_payroll_delivery_statement
        FOREIGN KEY (statement_id) REFERENCES payroll_statement (statement_id),
    CONSTRAINT fk_payroll_delivery_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_payroll_delivery_requester
        FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT uk_payroll_delivery_token UNIQUE (delivery_token),
    CONSTRAINT chk_payroll_delivery_status
        CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'DELIVERED', 'FAILED', 'SKIPPED')),
    INDEX idx_payroll_delivery_batch (batch_id, delivery_id),
    INDEX idx_payroll_delivery_payroll (payroll_id, requested_at),
    INDEX idx_payroll_delivery_statement (statement_id, requested_at)
);
