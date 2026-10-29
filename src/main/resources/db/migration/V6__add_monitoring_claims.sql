ALTER TABLE api_dependencies
    ADD COLUMN monitoring_claim_token VARCHAR(36),
    ADD COLUMN monitoring_claim_until TIMESTAMPTZ;

CREATE INDEX idx_api_dependencies_monitoring_claim
    ON api_dependencies (monitoring_claim_until)
    WHERE monitoring_claim_token IS NOT NULL;