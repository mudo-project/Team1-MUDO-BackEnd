CREATE TABLE corporate_cards (
    card_id BIGINT NOT NULL AUTO_INCREMENT,
    academy_id BIGINT NOT NULL,
    card_name VARCHAR(100) NOT NULL,
    card_company VARCHAR(100) NOT NULL,
    card_number_masked VARCHAR(30) NOT NULL,
    approval_template_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (card_id),
    CONSTRAINT fk_corporate_cards_academy FOREIGN KEY (academy_id) REFERENCES academy (academy_id),
    CONSTRAINT fk_corporate_cards_template FOREIGN KEY (approval_template_id) REFERENCES template (template_id),
    CONSTRAINT chk_corporate_cards_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE corporate_card_transactions (
    transaction_id BIGINT NOT NULL AUTO_INCREMENT,
    card_id BIGINT NOT NULL,
    approved_at DATETIME NOT NULL,
    approval_number VARCHAR(50) NOT NULL,
    merchant_name VARCHAR(200) NOT NULL,
    installment_months SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    amount BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_id),
    CONSTRAINT fk_card_transactions_card FOREIGN KEY (card_id) REFERENCES corporate_cards (card_id),
    CONSTRAINT uk_card_transactions_approval UNIQUE (card_id, approval_number),
    CONSTRAINT chk_card_transactions_amount CHECK (amount >= 0)
);

CREATE TABLE card_expenses (
    expense_id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    expense_category VARCHAR(30) NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    approval_document_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (expense_id),
    CONSTRAINT fk_card_expenses_transaction FOREIGN KEY (transaction_id) REFERENCES corporate_card_transactions (transaction_id),
    CONSTRAINT fk_card_expenses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_card_expenses_transaction UNIQUE (transaction_id)
);

CREATE INDEX idx_card_transactions_card_approved_at
    ON corporate_card_transactions (card_id, approved_at);

INSERT INTO permission (code, resource, action, description)
SELECT 'CORPORATE_CARD:EXPENSE', 'CORPORATE_CARD', 'EXPENSE', '법인카드 사용내역 조회 및 정산 처리'
WHERE NOT EXISTS (
    SELECT 1
    FROM permission
    WHERE code = 'CORPORATE_CARD:EXPENSE'
);
