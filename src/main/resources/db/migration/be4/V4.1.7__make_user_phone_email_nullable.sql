-- 계정 생성 시 원장이 직원 전체의 연락처를 일일이 입력하지 않아도 되도록,
-- phone_number/email을 선택 입력으로 바꾼다. 본인이 나중에
-- PATCH /api/users/me로 채워 넣을 수 있다.
ALTER TABLE `users`
  MODIFY COLUMN `phone_number` varchar(20) COLLATE utf8mb4_unicode_ci NULL,
  MODIFY COLUMN `email` varchar(100) COLLATE utf8mb4_unicode_ci NULL;
