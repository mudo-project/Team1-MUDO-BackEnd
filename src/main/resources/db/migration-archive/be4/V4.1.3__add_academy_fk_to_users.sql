-- users.academy_id는 academy 테이블 생성(V2.1.2) 이전부터 있던 컬럼이라 FK가 없었음.
-- 소속 학원이 남아있는 동안 academy 삭제를 막기 위해 기본 제약(RESTRICT)으로 연결.
ALTER TABLE users
    ADD CONSTRAINT fk_users_academy FOREIGN KEY (academy_id) REFERENCES academy (academy_id);
