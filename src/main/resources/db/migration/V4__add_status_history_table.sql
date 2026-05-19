CREATE TABLE application_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    old_status VARCHAR(50) NOT NULL,
    new_status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_application_status_history_old_status
        CHECK (old_status IN ('APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT chk_application_status_history_new_status
        CHECK (new_status IN ('APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN'))
);

CREATE INDEX idx_application_status_history_application_id
    ON application_status_history(application_id);
