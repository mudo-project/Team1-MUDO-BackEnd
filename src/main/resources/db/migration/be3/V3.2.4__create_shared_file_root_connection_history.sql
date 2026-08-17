-- 학원이 Google 계정을 A -> B -> A 처럼 여러 번 갈아탈 때, 각 계정이 마지막으로 쓰던 루트 폴더를
-- 기억해뒀다가 그 계정으로 다시 돌아오면 새로 만들지 않고 재사용하기 위한 이력 테이블.
-- shared_file_root(싱글턴, 현재 활성 상태만 가짐)와 달리 이 테이블은 이메일당 1행씩 계정 수만큼 쌓인다.
CREATE TABLE shared_file_root_connection_history (
    google_email VARCHAR(255) NOT NULL,
    google_root_folder_id VARCHAR(255) NOT NULL,
    last_connected_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_shared_file_root_connection_history PRIMARY KEY (google_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
