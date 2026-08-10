-- 프로젝트 전체가 단일 학원용으로 전환되면서 academy_id(멀티테넌시) 스코프를 더 이상 쓰지 않는다.
-- attendance_entry, message_template 테이블의 academy_id 컬럼과 관련 유니크 제약을 제거한다.
-- attendance_entry의 uk_attendance_entry_lecture_student_date는 이미 academy_id를 포함하지 않아 컬럼만 제거하면 된다.

ALTER TABLE `attendance_entry`
  DROP COLUMN `academy_id`;

ALTER TABLE `message_template`
  DROP INDEX `uk_message_template_academy_status`,
  DROP COLUMN `academy_id`,
  ADD UNIQUE KEY `uk_message_template_status` (`status`);
