CREATE TABLE resource_usage_event (
    resource_usage_event_id BIGINT NOT NULL AUTO_INCREMENT,
    resource_type VARCHAR(30) NOT NULL,
    feature VARCHAR(100) NOT NULL,
    amount BIGINT NOT NULL,
    unit VARCHAR(30) NOT NULL,
    provider VARCHAR(50) NULL,
    model_name VARCHAR(100) NULL,
    prompt_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (resource_usage_event_id),
    INDEX idx_resource_usage_event_occurred_resource (occurred_at, resource_type),
    INDEX idx_resource_usage_event_resource_feature_occurred (resource_type, feature, occurred_at)
);
