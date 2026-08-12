CREATE TABLE payroll_policy (
    payroll_policy_id BIGINT NOT NULL AUTO_INCREMENT,
    pay_day_type VARCHAR(20) NOT NULL,
    pay_day INT NULL,
    payment_month_offset INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (payroll_policy_id),
    CONSTRAINT chk_payroll_policy_day_type CHECK (pay_day_type IN ('FIXED_DAY', 'MONTH_END')),
    CONSTRAINT chk_payroll_policy_day CHECK (
        (pay_day_type = 'FIXED_DAY' AND pay_day BETWEEN 1 AND 31)
        OR (pay_day_type = 'MONTH_END' AND pay_day IS NULL)
    ),
    CONSTRAINT chk_payroll_policy_offset CHECK (payment_month_offset BETWEEN 0 AND 12)
);

CREATE TABLE statutory_policy (
    policy_id BIGINT NOT NULL AUTO_INCREMENT,
    policy_type VARCHAR(40) NOT NULL,
    rate DECIMAL(10, 6) NULL,
    employee_rate DECIMAL(10, 6) NULL,
    employer_rate DECIMAL(10, 6) NULL,
    minimum_base DECIMAL(15, 2) NULL,
    maximum_base DECIMAL(15, 2) NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_id),
    CONSTRAINT uk_statutory_policy_type_from UNIQUE (policy_type, effective_from),
    CONSTRAINT chk_statutory_policy_period CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE TABLE workplace_labor_scope (
    labor_scope_id BIGINT NOT NULL AUTO_INCREMENT,
    `year_month` DATE NOT NULL,
    regular_employee_count INT NOT NULL,
    is_five_or_more BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (labor_scope_id),
    CONSTRAINT uk_workplace_labor_scope_month UNIQUE (`year_month`),
    CONSTRAINT chk_workplace_labor_scope_count CHECK (regular_employee_count >= 0),
    CONSTRAINT chk_workplace_labor_scope_first_day CHECK (DAYOFMONTH(`year_month`) = 1)
);

CREATE TABLE employee_compensation (
    compensation_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    employment_type VARCHAR(20) NOT NULL,
    salary_type VARCHAR(20) NOT NULL,
    base_salary DECIMAL(15, 2) NULL,
    hourly_wage DECIMAL(15, 2) NULL,
    weekly_contract_hours DECIMAL(7, 2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (compensation_id),
    CONSTRAINT fk_employee_compensation_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_employee_compensation_user_from UNIQUE (user_id, effective_from),
    CONSTRAINT chk_employee_compensation_employment CHECK (employment_type IN ('REGULAR', 'FIXED_TERM', 'PART_TIME')),
    CONSTRAINT chk_employee_compensation_salary_type CHECK (salary_type IN ('MONTHLY', 'HOURLY')),
    CONSTRAINT chk_employee_compensation_amount CHECK (
        (salary_type = 'MONTHLY' AND base_salary IS NOT NULL AND base_salary >= 0)
        OR (salary_type = 'HOURLY' AND hourly_wage IS NOT NULL AND hourly_wage >= 0)
    ),
    CONSTRAINT chk_employee_compensation_weekly_hours CHECK
        (weekly_contract_hours >= 0 AND weekly_contract_hours <= 168),
    CONSTRAINT chk_employee_compensation_period CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX idx_employee_compensation_period
    ON employee_compensation (user_id, effective_from, effective_to);

CREATE TABLE employee_fixed_allowance (
    allowance_id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    allowance_type VARCHAR(30) NOT NULL,
    allowance_name VARCHAR(100) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (allowance_id),
    CONSTRAINT fk_employee_fixed_allowance_user FOREIGN KEY (employee_id) REFERENCES users (id),
    CONSTRAINT uk_employee_fixed_allowance_from UNIQUE
        (employee_id, allowance_type, allowance_name, effective_from),
    CONSTRAINT chk_employee_fixed_allowance_type CHECK
        (allowance_type IN ('MEAL', 'POSITION', 'DUTY', 'TRANSPORTATION', 'OTHER')),
    CONSTRAINT chk_employee_fixed_allowance_amount CHECK (amount >= 0),
    CONSTRAINT chk_employee_fixed_allowance_period CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE TABLE employee_pay_basis (
    pay_basis_id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    ordinary_hourly_wage DECIMAL(15, 2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (pay_basis_id),
    CONSTRAINT fk_employee_pay_basis_user FOREIGN KEY (employee_id) REFERENCES users (id),
    CONSTRAINT uk_employee_pay_basis_user_from UNIQUE (employee_id, effective_from),
    CONSTRAINT chk_employee_pay_basis_wage CHECK (ordinary_hourly_wage > 0),
    CONSTRAINT chk_employee_pay_basis_period CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE TABLE social_insurance_status (
    insurance_status_id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    national_pension_status VARCHAR(20) NOT NULL,
    health_insurance_status VARCHAR(20) NOT NULL,
    employment_insurance_status VARCHAR(20) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    exemption_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (insurance_status_id),
    CONSTRAINT fk_social_insurance_status_user FOREIGN KEY (employee_id) REFERENCES users (id),
    CONSTRAINT uk_social_insurance_status_user_from UNIQUE (employee_id, effective_from),
    CONSTRAINT chk_social_insurance_pension_status CHECK (national_pension_status IN ('ENROLLED', 'EXEMPT', 'NOT_APPLICABLE')),
    CONSTRAINT chk_social_insurance_health_status CHECK (health_insurance_status IN ('ENROLLED', 'EXEMPT', 'NOT_APPLICABLE')),
    CONSTRAINT chk_social_insurance_employment_status CHECK (employment_insurance_status IN ('ENROLLED', 'EXEMPT', 'NOT_APPLICABLE')),
    CONSTRAINT chk_social_insurance_status_period CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE TABLE social_insurance_assessment (
    assessment_id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    `year_month` DATE NOT NULL,
    national_pension_amount DECIMAL(15, 2) NOT NULL,
    health_insurance_amount DECIMAL(15, 2) NOT NULL,
    long_term_care_amount DECIMAL(15, 2) NOT NULL,
    employment_insurance_amount DECIMAL(15, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (assessment_id),
    CONSTRAINT fk_social_insurance_assessment_user FOREIGN KEY (employee_id) REFERENCES users (id),
    CONSTRAINT uk_social_insurance_assessment_month UNIQUE (employee_id, `year_month`),
    CONSTRAINT chk_social_insurance_assessment_first_day CHECK (DAYOFMONTH(`year_month`) = 1),
    CONSTRAINT chk_social_insurance_assessment_amounts CHECK (
        national_pension_amount >= 0 AND health_insurance_amount >= 0
        AND long_term_care_amount >= 0 AND employment_insurance_amount >= 0
    )
);

CREATE TABLE tax_assessment (
    tax_assessment_id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    `year_month` DATE NOT NULL,
    income_tax_amount DECIMAL(15, 2) NOT NULL,
    local_income_tax_amount DECIMAL(15, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (tax_assessment_id),
    CONSTRAINT fk_tax_assessment_user FOREIGN KEY (employee_id) REFERENCES users (id),
    CONSTRAINT uk_tax_assessment_month UNIQUE (employee_id, `year_month`),
    CONSTRAINT chk_tax_assessment_first_day CHECK (DAYOFMONTH(`year_month`) = 1),
    CONSTRAINT chk_tax_assessment_amounts CHECK
        (income_tax_amount >= 0 AND local_income_tax_amount >= 0)
);

CREATE TABLE payroll (
    payroll_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    payroll_year_month DATE NOT NULL,
    scheduled_pay_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_earnings DECIMAL(15, 2) NULL,
    total_deductions DECIMAL(15, 2) NULL,
    net_pay DECIMAL(15, 2) NULL,
    revision_no INT NOT NULL DEFAULT 1,
    original_payroll_id BIGINT NULL,
    memo VARCHAR(1000) NULL,
    calculated_at DATETIME NULL,
    confirmed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (payroll_id),
    CONSTRAINT fk_payroll_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_payroll_original FOREIGN KEY (original_payroll_id) REFERENCES payroll (payroll_id),
    CONSTRAINT uk_payroll_user_month_revision UNIQUE (user_id, payroll_year_month, revision_no),
    CONSTRAINT chk_payroll_status CHECK (status IN ('DRAFT', 'CALCULATED', 'CONFIRMED')),
    CONSTRAINT chk_payroll_year_month_first_day CHECK (DAYOFMONTH(payroll_year_month) = 1),
    CONSTRAINT chk_payroll_revision CHECK (revision_no >= 1)
);

CREATE INDEX idx_payroll_month_status ON payroll (payroll_year_month, status);

CREATE TABLE payroll_item (
    payroll_item_id BIGINT NOT NULL AUTO_INCREMENT,
    payroll_id BIGINT NOT NULL,
    item_category VARCHAR(20) NOT NULL,
    item_type VARCHAR(40) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    original_amount DECIMAL(15, 2) NULL,
    is_adjusted BOOLEAN NOT NULL DEFAULT FALSE,
    adjustment_reason VARCHAR(500) NULL,
    calculation_formula VARCHAR(500) NULL,
    calculation_basis JSON NULL,
    display_order INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (payroll_item_id),
    CONSTRAINT fk_payroll_item_payroll FOREIGN KEY (payroll_id) REFERENCES payroll (payroll_id),
    CONSTRAINT chk_payroll_item_amount CHECK (amount >= 0),
    CONSTRAINT chk_payroll_item_category CHECK (item_category IN ('EARNING', 'DEDUCTION')),
    CONSTRAINT chk_payroll_item_source CHECK
        (source_type IN ('CONTRACT', 'ATTENDANCE', 'MOCK_INSURANCE', 'MOCK_TAX', 'MANUAL'))
);

CREATE TABLE payroll_attendance_snapshot (
    snapshot_id BIGINT NOT NULL AUTO_INCREMENT,
    payroll_id BIGINT NOT NULL,
    work_days INT NOT NULL,
    work_hours DECIMAL(10, 4) NOT NULL,
    overtime_hours DECIMAL(10, 4) NOT NULL,
    night_hours DECIMAL(10, 4) NOT NULL,
    holiday_hours DECIMAL(10, 4) NOT NULL,
    paid_leave_hours DECIMAL(10, 4) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (snapshot_id),
    CONSTRAINT fk_payroll_attendance_snapshot_payroll FOREIGN KEY (payroll_id) REFERENCES payroll (payroll_id),
    CONSTRAINT uk_payroll_attendance_snapshot UNIQUE (payroll_id)
);

CREATE TABLE payroll_compensation_snapshot (
    snapshot_id BIGINT NOT NULL AUTO_INCREMENT,
    payroll_id BIGINT NOT NULL,
    applied_from DATE NOT NULL,
    applied_to DATE NOT NULL,
    employment_type VARCHAR(20) NOT NULL,
    salary_type VARCHAR(20) NOT NULL,
    base_salary DECIMAL(15, 2) NULL,
    hourly_wage DECIMAL(15, 2) NULL,
    ordinary_hourly_wage DECIMAL(15, 2) NOT NULL,
    weekly_contract_hours DECIMAL(7, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (snapshot_id),
    CONSTRAINT fk_payroll_compensation_snapshot_payroll FOREIGN KEY (payroll_id) REFERENCES payroll (payroll_id)
);

CREATE TABLE payroll_rule_snapshot (
    snapshot_id BIGINT NOT NULL AUTO_INCREMENT,
    payroll_id BIGINT NOT NULL,
    labor_scope_id BIGINT NOT NULL,
    is_five_or_more BOOLEAN NOT NULL,
    overtime_multiplier DECIMAL(8, 4) NOT NULL,
    night_multiplier DECIMAL(8, 4) NOT NULL,
    holiday_under_8_multiplier DECIMAL(8, 4) NOT NULL,
    holiday_over_8_multiplier DECIMAL(8, 4) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (snapshot_id),
    CONSTRAINT fk_payroll_rule_snapshot_payroll FOREIGN KEY (payroll_id) REFERENCES payroll (payroll_id),
    CONSTRAINT fk_payroll_rule_snapshot_scope FOREIGN KEY (labor_scope_id) REFERENCES workplace_labor_scope (labor_scope_id),
    CONSTRAINT uk_payroll_rule_snapshot UNIQUE (payroll_id)
);

CREATE TABLE payroll_statement (
    statement_id BIGINT NOT NULL AUTO_INCREMENT,
    payroll_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    object_key VARCHAR(500) NULL,
    content_type VARCHAR(100) NULL,
    file_size BIGINT NULL,
    checksum VARCHAR(128) NULL,
    generated_at DATETIME NULL,
    failure_reason VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (statement_id),
    CONSTRAINT fk_payroll_statement_payroll FOREIGN KEY (payroll_id) REFERENCES payroll (payroll_id),
    CONSTRAINT uk_payroll_statement_payroll UNIQUE (payroll_id),
    CONSTRAINT chk_payroll_statement_status CHECK (status IN ('PENDING', 'READY', 'FAILED'))
);

INSERT INTO permission (code, resource, action, description)
SELECT 'PAYROLL:MANAGE', 'PAYROLL', 'MANAGE', '급여 및 급여명세서 조회·계산·확정·관리'
WHERE NOT EXISTS (
    SELECT 1 FROM permission WHERE code = 'PAYROLL:MANAGE'
);
