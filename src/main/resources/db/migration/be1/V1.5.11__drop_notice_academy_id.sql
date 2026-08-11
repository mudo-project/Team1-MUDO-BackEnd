-- 프로젝트 전체가 단일 학원용으로 전환되면서 academy_id(멀티테넌시) 스코프를 더 이상 쓰지 않는다.
-- notice 테이블의 academy_id 컬럼을 제거한다(인덱스/제약에 포함되어 있지 않아 컬럼만 제거하면 된다).

ALTER TABLE `notice`
  DROP COLUMN `academy_id`;
