-- 프로젝트 전체가 단일 학원용으로 전환되면서 academy_id(멀티테넌시) 스코프를 더 이상 쓰지 않는다.
-- file_metadata 테이블의 academy_id 컬럼과 관련 인덱스를 제거한다.
-- (V1.5.6에서 다른 학원 사용자의 IDOR 방지 목적으로 추가했던 컬럼이지만, 단일 학원에서는
--  모든 사용자의 academy_id가 항상 같은 값이라 더 이상 실질적인 격리 효과가 없다.)

ALTER TABLE `file_metadata`
  DROP INDEX `idx_file_metadata_academy_id`;

ALTER TABLE `file_metadata`
  DROP COLUMN `academy_id`;
