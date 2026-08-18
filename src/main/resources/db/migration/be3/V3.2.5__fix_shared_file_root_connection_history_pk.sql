-- V3.2.4에서 google_email을 PK로 잡았던 걸 이 프로젝트 컨벤션(대리키 Long PK + 비즈니스 키는
-- 별도 UNIQUE 제약, 예: workspace.active_name)에 맞게 정정한다. V3.2.4가 아직 어떤 환경에도
-- 배포되지 않은 상태라(develop 미병합 시점 기준) 데이터 보존 없이 재생성해도 안전하다.
DROP TABLE shared_file_root_connection_history;

CREATE TABLE shared_file_root_connection_history (
    shared_file_root_connection_history_id BIGINT NOT NULL AUTO_INCREMENT,
    google_email VARCHAR(255) NOT NULL,
    google_root_folder_id VARCHAR(255) NOT NULL,
    last_connected_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_shared_file_root_connection_history PRIMARY KEY (shared_file_root_connection_history_id),
    CONSTRAINT uk_shared_file_root_connection_history_google_email UNIQUE (google_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
