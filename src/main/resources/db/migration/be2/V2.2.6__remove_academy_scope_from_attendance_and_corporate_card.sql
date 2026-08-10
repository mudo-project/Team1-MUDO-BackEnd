ALTER TABLE academy_wifi_ip
    DROP FOREIGN KEY fk_academy_wifi_ip_academy,
    DROP INDEX uk_academy_wifi_ip,
    DROP COLUMN academy_id,
    ADD CONSTRAINT uk_academy_wifi_ip UNIQUE (ip_address);

ALTER TABLE attendance_policies
    DROP FOREIGN KEY fk_attendance_policies_academy,
    DROP INDEX uk_attendance_policies_academy,
    DROP COLUMN academy_id;

ALTER TABLE attendance_record
    DROP FOREIGN KEY fk_attendance_record_academy,
    DROP INDEX uk_attendance_record_academy_user_date,
    DROP INDEX fk_attendance_record_academy,
    DROP COLUMN academy_id,
    ADD CONSTRAINT uk_attendance_record_user_date UNIQUE (user_id, work_date);

ALTER TABLE leave_request
    DROP INDEX idx_leave_request_academy_status_date,
    DROP INDEX idx_leave_request_user_status_date,
    DROP COLUMN academy_id,
    ADD INDEX idx_leave_request_status_date (status, start_date, end_date),
    ADD INDEX idx_leave_request_user_status_date (user_id, status, start_date, end_date);

ALTER TABLE leave_grant
    DROP FOREIGN KEY fk_leave_grant_academy,
    DROP INDEX uk_leave_grant_user_date,
    DROP INDEX idx_leave_grant_user_expiration,
    DROP COLUMN academy_id,
    ADD CONSTRAINT uk_leave_grant_user_date UNIQUE (user_id, grant_date),
    ADD INDEX idx_leave_grant_user_expiration (user_id, expiration_date);

ALTER TABLE attendance_correction_request
    DROP FOREIGN KEY fk_attendance_correction_request_academy,
    DROP INDEX uk_attendance_correction_request_pending,
    DROP INDEX idx_attendance_correction_request_user_requested,
    DROP COLUMN academy_id,
    ADD CONSTRAINT uk_attendance_correction_request_pending
        UNIQUE (user_id, work_date, pending_guard),
    ADD INDEX idx_attendance_correction_request_user_requested (user_id, requested_at DESC);

ALTER TABLE corporate_cards
    DROP FOREIGN KEY fk_corporate_cards_academy,
    DROP COLUMN academy_id;
