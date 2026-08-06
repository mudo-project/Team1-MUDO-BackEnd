ALTER TABLE calendar_events
    ADD INDEX idx_calendar_events_academy_start_at (academy_id, event_start_at);
