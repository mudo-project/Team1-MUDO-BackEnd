CREATE TABLE data_import_job (
    id BIGINT NOT NULL AUTO_INCREMENT,
    academy_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    source_file_names LONGTEXT NOT NULL,
    draft_json LONGTEXT NOT NULL,
    result_json LONGTEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_data_import_job_academy_created_at (academy_id, created_at),
    INDEX idx_data_import_job_status (status)
);
