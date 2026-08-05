ALTER TABLE attendance_record
    ADD COLUMN academy_id BIGINT NULL AFTER user_id,
    ADD COLUMN clock_in_note VARCHAR(255) NULL AFTER clock_in_at,
    ADD COLUMN clock_out_note VARCHAR(255) NULL AFTER clock_out_at;

UPDATE attendance_record ar
JOIN users u ON u.id = ar.user_id
SET ar.academy_id = u.academy_id,
    ar.clock_in_note = ar.note;

ALTER TABLE attendance_record
    MODIFY COLUMN academy_id BIGINT NOT NULL,
    DROP COLUMN note,
    DROP INDEX uk_attendance_record_user_date,
    ADD CONSTRAINT fk_attendance_record_academy
        FOREIGN KEY (academy_id) REFERENCES academy (academy_id),
    ADD CONSTRAINT uk_attendance_record_academy_user_date
        UNIQUE (academy_id, user_id, work_date);
