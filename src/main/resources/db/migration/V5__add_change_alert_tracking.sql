-- Stable identity for each detected change.
ALTER TABLE api_changes
    ADD COLUMN fingerprint VARCHAR(64);

-- Track whether an alert needs to be sent or has been delivered.
ALTER TABLE api_changes
    ADD COLUMN alert_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN alert_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN alert_last_error TEXT,
    ADD COLUMN alert_sent_at TIMESTAMPTZ;

-- Existing records have no fingerprint yet. New changes will populate it.
CREATE INDEX idx_api_changes_alert_pending
    ON api_changes (alert_status, detected_at);