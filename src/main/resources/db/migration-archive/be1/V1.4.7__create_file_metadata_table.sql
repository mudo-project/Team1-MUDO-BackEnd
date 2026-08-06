CREATE TABLE file_metadata (
    id BIGINT NOT NULL AUTO_INCREMENT,
    object_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_metadata_object_key (object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
