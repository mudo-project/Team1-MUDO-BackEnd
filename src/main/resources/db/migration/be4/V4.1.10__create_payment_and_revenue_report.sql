CREATE TABLE payment (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    amount INT NOT NULL,
    paid_at DATETIME NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_payment_enrollment_id ON payment (enrollment_id);
CREATE INDEX idx_payment_paid_at ON payment (paid_at);

CREATE TABLE revenue_report (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_month DATE NOT NULL,
    report TEXT NOT NULL,
    data_snapshot TEXT NOT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_revenue_report_target_month UNIQUE (target_month)
);
