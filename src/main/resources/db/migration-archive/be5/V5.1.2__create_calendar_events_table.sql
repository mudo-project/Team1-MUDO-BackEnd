CREATE TABLE calendar_events (
    event_id BIGINT NOT NULL AUTO_INCREMENT,
    academy_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NULL,
    event_start_at DATETIME NOT NULL,
    event_end_at DATETIME NULL,
    is_all_day BOOLEAN NOT NULL DEFAULT FALSE,
    color VARCHAR(20) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_calendar_events PRIMARY KEY (event_id),
    CONSTRAINT fk_calendar_events_academy
        FOREIGN KEY (academy_id) REFERENCES academy(academy_id),
    CONSTRAINT fk_calendar_events_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
