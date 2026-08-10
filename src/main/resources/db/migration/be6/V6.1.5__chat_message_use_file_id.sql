-- file_url에 실제 값이 남아있는 채로 배포하면 DROP COLUMN으로 그 값이 영구 유실된다.
-- MySQL은 스크립트 최상단에서 IF/THEN을 못 쓰므로, 존재하지 않는 테이블을 참조하는
-- 동적 SQL을 만들어 강제로 에러를 내는 방식으로 배포를 중단시킨다(count가 0이면
-- 'SELECT 1'만 실행되어 정상 진행).
SET @file_url_count = (
  SELECT COUNT(*) FROM `chat_message` WHERE `file_url` IS NOT NULL AND `file_url` <> ''
);
SET @guard_sql = IF(@file_url_count = 0,
  'SELECT 1',
  'SELECT 1 FROM `ABORT_MIGRATION_chat_message_file_url_has_data_backfill_file_id_first`'
);
PREPARE guard_stmt FROM @guard_sql;
EXECUTE guard_stmt;
DEALLOCATE PREPARE guard_stmt;

ALTER TABLE `chat_message`
  ADD COLUMN `file_id` BIGINT NULL AFTER `chat_room_id`,
  ADD CONSTRAINT `fk_chat_message_file` FOREIGN KEY (`file_id`) REFERENCES `file_metadata` (`id`),
  DROP COLUMN `file_url`;
