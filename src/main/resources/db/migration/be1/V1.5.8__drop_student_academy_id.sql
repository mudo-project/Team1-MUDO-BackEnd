-- 프로젝트 전체가 단일 학원용으로 전환되면서 academy_id(멀티테넌시) 스코프를 더 이상 쓰지 않는다.
-- student, student_enrollment 테이블의 academy_id 컬럼과 관련 인덱스/유니크 제약을 제거한다.

ALTER TABLE `student`
  DROP INDEX `idx_student_academy_name`,
  DROP COLUMN `academy_id`,
  ADD INDEX `idx_student_name` (`name`, `student_id`);

ALTER TABLE `student_enrollment`
  DROP INDEX `uk_student_enrollment_student_lecture`,
  DROP INDEX `idx_student_enrollment_student_status`,
  DROP INDEX `idx_student_enrollment_lecture_status`,
  DROP COLUMN `academy_id`,
  ADD UNIQUE KEY `uk_student_enrollment_student_lecture` (`student_id`, `lecture_id`),
  ADD INDEX `idx_student_enrollment_student_status` (`student_id`, `status`),
  ADD INDEX `idx_student_enrollment_lecture_status` (`lecture_id`, `status`);
