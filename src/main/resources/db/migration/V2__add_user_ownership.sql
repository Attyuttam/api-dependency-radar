
-- V2: Introduce user accounts and dependency ownership.
-- Existing dependencies are preserved; ownership is finalized later.

CREATE TABLE app_users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Keep ownership nullable until a real account has been registered
-- and existing dependencies have been assigned to their owners.
ALTER TABLE api_dependencies
    ADD COLUMN owner_id VARCHAR(36);

ALTER TABLE api_dependencies
    DROP CONSTRAINT uk_api_dependencies_source_url;

ALTER TABLE api_dependencies
    ADD CONSTRAINT uk_api_dependencies_owner_source_url
    UNIQUE (owner_id, source_url);
