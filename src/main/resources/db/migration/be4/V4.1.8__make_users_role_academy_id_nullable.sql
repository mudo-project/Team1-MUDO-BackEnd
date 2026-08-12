-- academyId 스코핑 제거(Phase 2): users/role 도메인 코드는 이제 academy_id를 읽거나 쓰지 않는다.
-- 컬럼 자체는 DROP하지 않는다 — messenger.ChatMemberInfoEntity가 "users" 테이블의 academy_id에
-- 여전히 매핑돼 있어서(shim), 지금 DROP하면 messenger가 깨진다. messenger 쪽 shim 정리가
-- 끝난 뒤 별도 후속 마이그레이션에서 실제로 DROP한다.
ALTER TABLE `role` DROP FOREIGN KEY `fk_role_academy`;
ALTER TABLE `role` DROP INDEX `uk_role_academy_name`;
ALTER TABLE `role` MODIFY COLUMN `academy_id` BIGINT NULL;
ALTER TABLE `role` ADD UNIQUE KEY `uk_role_name` (`name`);

ALTER TABLE `users` DROP FOREIGN KEY `fk_users_academy`;
ALTER TABLE `users` MODIFY COLUMN `academy_id` BIGINT NULL;
