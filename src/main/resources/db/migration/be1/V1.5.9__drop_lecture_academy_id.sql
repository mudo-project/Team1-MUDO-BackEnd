-- 프로젝트 전체가 단일 학원용으로 전환되면서 academy_id(멀티테넌시) 스코프를 더 이상 쓰지 않는다.
-- term/subject/classroom/lecture 테이블의 academy_id 컬럼과 관련 유니크 제약을 제거한다.

ALTER TABLE `term`
  DROP INDEX `uk_term_academy_name`,
  DROP COLUMN `academy_id`,
  ADD UNIQUE KEY `uk_term_name` (`name`);

ALTER TABLE `subject`
  DROP INDEX `uk_subject_academy_name`,
  DROP COLUMN `academy_id`,
  ADD UNIQUE KEY `uk_subject_name` (`name`);

ALTER TABLE `classroom`
  DROP INDEX `uk_classroom_academy_name`,
  DROP COLUMN `academy_id`,
  ADD UNIQUE KEY `uk_classroom_name` (`name`);

ALTER TABLE `lecture`
  DROP COLUMN `academy_id`;
