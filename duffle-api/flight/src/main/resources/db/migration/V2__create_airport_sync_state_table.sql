CREATE TABLE airport_sync_state (
    job_name VARCHAR(64) PRIMARY KEY,
    last_cursor VARCHAR(512),
    status VARCHAR(16) NOT NULL DEFAULT 'IDLE',
    last_run_started_at TIMESTAMP,
    last_run_finished_at TIMESTAMP,
    last_error TEXT
);
