CREATE TABLE academy_wifi_ip (
    wifi_ip_id BIGINT NOT NULL AUTO_INCREMENT,
    academy_id BIGINT NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    note VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_academy_wifi_ip PRIMARY KEY (wifi_ip_id),
    CONSTRAINT fk_academy_wifi_ip_academy
        FOREIGN KEY (academy_id) REFERENCES academy(academy_id) ON DELETE CASCADE,
    CONSTRAINT uk_academy_wifi_ip UNIQUE (academy_id, ip_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attendance_record (
    attendance_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    clock_in_at DATETIME NULL,
    clock_out_at DATETIME NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_attendance_record PRIMARY KEY (attendance_id),
    CONSTRAINT fk_attendance_record_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_attendance_record_user_date UNIQUE (user_id, work_date),
    CONSTRAINT chk_attendance_record_clock_out CHECK (
        clock_out_at IS NULL
        OR (clock_in_at IS NOT NULL AND clock_out_at >= clock_in_at)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_attendance_record_work_date
    ON attendance_record (work_date);
