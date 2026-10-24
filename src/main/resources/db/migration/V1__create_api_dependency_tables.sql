
CREATE TABLE api_dependencies (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    provider_name VARCHAR(120),
    source_url VARCHAR(2048) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    description VARCHAR(1000),
    baseline_spec TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_checked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_api_dependencies_source_url UNIQUE (source_url)
);

CREATE INDEX idx_api_dependencies_created_at
    ON api_dependencies (created_at);

CREATE TABLE api_changes (
    id VARCHAR(36) PRIMARY KEY,
    dependency_id VARCHAR(36) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    change_type VARCHAR(80) NOT NULL,
    endpoint VARCHAR(500),
    description TEXT NOT NULL,
    breaking BOOLEAN NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_api_changes_dependency
        FOREIGN KEY (dependency_id)
        REFERENCES api_dependencies (id)
);

CREATE INDEX idx_api_changes_dependency_detected
    ON api_changes (dependency_id, detected_at);
