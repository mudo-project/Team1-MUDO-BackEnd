ALTER TABLE calendar_events
    DROP FOREIGN KEY fk_calendar_events_academy;

ALTER TABLE calendar_events
    DROP INDEX idx_calendar_events_academy_start_at,
    DROP COLUMN academy_id,
    ADD INDEX idx_calendar_events_start_at (event_start_at);

ALTER TABLE google_account_connection
    DROP FOREIGN KEY fk_google_account_connection_academy;

ALTER TABLE google_account_connection
    DROP INDEX uk_google_account_connection_academy,
    DROP COLUMN academy_id,
    ADD COLUMN singleton_key TINYINT NOT NULL DEFAULT 1,
    ADD CONSTRAINT uk_google_account_connection_singleton UNIQUE (singleton_key),
    ADD CONSTRAINT chk_google_account_connection_singleton CHECK (singleton_key = 1);

ALTER TABLE timetable_set
    DROP INDEX uk_timetable_set_academy_name,
    DROP COLUMN academy_id,
    ADD CONSTRAINT uk_timetable_set_name UNIQUE (name);
