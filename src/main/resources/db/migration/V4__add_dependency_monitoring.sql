-- Track the outcome of the most recent monitoring check.
ALTER TABLE api_dependencies
    ADD COLUMN last_check_success BOOLEAN,
    ADD COLUMN last_check_error TEXT,
    ADD COLUMN next_check_at TIMESTAMPTZ;

-- Schedule existing dependencies for their first automated check.
UPDATE api_dependencies
SET next_check_at = CURRENT_TIMESTAMP;

-- Support efficient selection of dependencies due for checking.
CREATE INDEX idx_api_dependencies_next_check
    ON api_dependencies (next_check_at)
    WHERE next_check_at IS NOT NULL;