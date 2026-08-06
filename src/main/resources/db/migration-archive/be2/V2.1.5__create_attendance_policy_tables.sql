CREATE TABLE attendance_policies (
    policy_id BIGINT NOT NULL AUTO_INCREMENT,
    academy_id BIGINT NOT NULL,
    default_start_time TIME NOT NULL,
    default_end_time TIME NOT NULL,
    late_grace_minutes SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    weekday_exception_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_attendance_policies PRIMARY KEY (policy_id),
    CONSTRAINT uk_attendance_policies_academy UNIQUE (academy_id),
    CONSTRAINT fk_attendance_policies_academy
        FOREIGN KEY (academy_id) REFERENCES academy(academy_id) ON DELETE CASCADE,
    CONSTRAINT chk_attendance_policies_grace_minutes
        CHECK (late_grace_minutes <= 180),
    CONSTRAINT chk_attendance_policies_default_time
        CHECK (default_start_time <> default_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attendance_policy_weekdays (
    policy_id BIGINT NOT NULL,
    day_of_week TINYINT UNSIGNED NOT NULL,
    is_workday BOOLEAN NOT NULL DEFAULT TRUE,
    start_time TIME NULL,
    end_time TIME NULL,
    CONSTRAINT pk_attendance_policy_weekdays PRIMARY KEY (policy_id, day_of_week),
    CONSTRAINT fk_attendance_policy_weekdays_policy
        FOREIGN KEY (policy_id) REFERENCES attendance_policies(policy_id) ON DELETE CASCADE,
    CONSTRAINT chk_attendance_policy_weekdays_day
        CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_attendance_policy_weekdays_time
        CHECK (
            (
                is_workday = FALSE
                AND start_time IS NULL
                AND end_time IS NULL
            )
            OR
            (
                is_workday = TRUE
                AND (
                    (
                        start_time IS NULL
                        AND end_time IS NULL
                    )
                    OR
                    (
                        start_time IS NOT NULL
                        AND end_time IS NOT NULL
                        AND start_time <> end_time
                    )
                )
            )
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
