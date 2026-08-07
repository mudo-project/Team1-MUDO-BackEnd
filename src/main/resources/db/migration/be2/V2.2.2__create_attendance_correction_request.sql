CREATE TABLE attendance_correction_request (
    request_id BIGINT NOT NULL AUTO_INCREMENT,
    academy_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    attendance_id BIGINT NULL,
    work_date DATE NOT NULL,

    request_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    original_clock_in_at DATETIME NULL,
    original_clock_out_at DATETIME NULL,
    original_clock_in_note VARCHAR(255) NULL,
    original_clock_out_note VARCHAR(255) NULL,

    requested_clock_in_at DATETIME NULL,
    requested_clock_out_at DATETIME NULL,
    requested_clock_in_note VARCHAR(255) NULL,
    requested_clock_out_note VARCHAR(255) NULL,

    reason VARCHAR(500) NOT NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    processed_by BIGINT NULL,
    rejection_reason VARCHAR(500) NULL,

    pending_guard TINYINT
        GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN 1 ELSE NULL END) STORED,

    CONSTRAINT pk_attendance_correction_request
        PRIMARY KEY (request_id),

    CONSTRAINT fk_attendance_correction_request_academy
        FOREIGN KEY (academy_id)
        REFERENCES academy (academy_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_attendance_correction_request_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT fk_attendance_correction_request_attendance
        FOREIGN KEY (attendance_id)
        REFERENCES attendance_record (attendance_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_attendance_correction_request_processor
        FOREIGN KEY (processed_by)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT uk_attendance_correction_request_pending
        UNIQUE (academy_id, user_id, work_date, pending_guard),

    CONSTRAINT chk_attendance_correction_request_type
        CHECK (request_type IN (
            'CLOCK_IN_TIME', 'CLOCK_OUT_TIME', 'MISSING_RECORD',
            'CLOCK_IN_NOTE', 'CLOCK_OUT_NOTE'
        )),

    CONSTRAINT chk_attendance_correction_request_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_attendance_correction_request_user_requested
    ON attendance_correction_request (academy_id, user_id, requested_at DESC);
