ALTER TABLE attendance_record
    ADD COLUMN clock_out_type VARCHAR(20) NULL AFTER clock_out_note,
    ADD CONSTRAINT chk_attendance_record_clock_out_type
        CHECK (
            clock_out_type IS NULL
            OR clock_out_type IN ('NORMAL', 'OVERTIME')
        );
