-- 프로젝트 전체가 단일 학원용으로 전환되면서 academy_id(멀티테넌시) 스코프를 더 이상 쓰지 않는다.
-- data_import_job 테이블의 academy_id 컬럼과 관련 인덱스를 제거한다.

ALTER TABLE `data_import_job`
  DROP INDEX `idx_data_import_job_academy_created_at`;

ALTER TABLE `data_import_job`
  DROP COLUMN `academy_id`;

ALTER TABLE `data_import_job`
  ADD INDEX `idx_data_import_job_created_by_created_at` (`created_by`, `created_at`);
