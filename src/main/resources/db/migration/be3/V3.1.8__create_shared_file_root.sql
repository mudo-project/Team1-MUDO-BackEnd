-- 배포 단위(학원)당 시스템 루트 설정 한 건만 저장한다. 파일·폴더 목록은 담지 않으며
-- Google Drive를 단일 원본으로 사용한다. PK를 1로 고정해 행이 둘 이상 생기지 않게 막는다.
CREATE TABLE shared_file_root (
    shared_file_root_id TINYINT NOT NULL,
    google_root_folder_id VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_shared_file_root PRIMARY KEY (shared_file_root_id),
    CONSTRAINT chk_shared_file_root_singleton CHECK (shared_file_root_id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;