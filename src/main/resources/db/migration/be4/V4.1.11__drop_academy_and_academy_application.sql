-- 프로젝트가 단일 학원용으로 전환되면서 academy/academy_application은 순수 죽은 스키마가
-- 됐다(매핑된 JPA 엔티티도, 참조하는 코드도 없음 — 프로덕션 코드는 이미 이전 커밋에서 삭제됨).
-- academy에 걸린 FK 2개(user_id -> users, application_id -> academy_application)는 academy
-- 자신에 걸려있어서 테이블 드롭과 함께 자동으로 정리된다.
DROP TABLE academy;
DROP TABLE academy_application;

-- users.academy_id/role.academy_id는 V4.1.8에서 FK만 끊고 컬럼은 남겨뒀었다(당시 messenger의
-- academy_id shim이 이 컬럼에 매핑돼 있어서). 그 shim은 이후 완전히 제거됐고(431f68b2,
-- b98b75ec) 더 이상 어떤 코드도 이 컬럼을 읽거나 쓰지 않아 이제 안전하게 제거한다.
ALTER TABLE users DROP COLUMN academy_id;
ALTER TABLE role DROP COLUMN academy_id;
