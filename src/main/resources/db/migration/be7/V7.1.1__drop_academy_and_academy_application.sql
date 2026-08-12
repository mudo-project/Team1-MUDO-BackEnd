-- 원래 be4/V4.1.11로 작성했다가 CI의 FlywayMigrationFromScratchTest에서 실패해 be7/V7.1.1로
-- 재조정했다(팀 승인, be7 번호대는 현재 아무도 쓰지 않음). 원인: 빈 DB에서 버전 순서대로
-- 재생하면 V4.x 시점엔 calendar_events(baseline)와 google_account_connection(be5/V5.1.1)의
-- academy FK가 아직 살아있고, 그 FK들은 be5/V5.1.8(이미 develop에 머지·적용된 파일이라 수정 불가)
-- 에서야 끊긴다. DROP TABLE academy가 성공하려면 이 마이그레이션이 V5.1.8보다 뒤에 와야 해서,
-- be5/be6(최고 버전 V6.1.6)보다 확실히 높은 be7로 옮겼다(docs/DATABASE.md "충돌 발생 시 대응" 절차).
--
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
