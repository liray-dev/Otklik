ALTER TABLE campaigns
    ADD COLUMN anonymity_mode VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN scale_max INT NOT NULL DEFAULT 10,
    ADD COLUMN expected_duration_days INT,
    ADD COLUMN organizer_id UUID REFERENCES users(id);

UPDATE campaigns SET mode = 'PEER_TO_PEER' WHERE mode = 'P2P';

ALTER TABLE evaluation_criteria
    DROP COLUMN max_score,
    ADD COLUMN weight NUMERIC(5,2) NOT NULL DEFAULT 0,
    ADD COLUMN position INT NOT NULL DEFAULT 0;

ALTER TABLE works
    DROP COLUMN file_path,
    ADD COLUMN title VARCHAR(500) NOT NULL DEFAULT 'Работа',
    ADD COLUMN external_link VARCHAR(1000),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE works SET status = 'UPLOADED' WHERE status NOT IN ('UPLOADED','IN_QUEUE','UNDER_REVIEW','REVIEWED','NEEDS_REVISION');

ALTER TABLE work_assignments
    ADD COLUMN taken_at TIMESTAMP,
    ADD COLUMN completed_at TIMESTAMP,
    ADD CONSTRAINT uk_work_reviewer UNIQUE (work_id, reviewer_id);

UPDATE work_assignments SET status = 'TAKEN' WHERE status = 'ASSIGNED';

ALTER TABLE reviews
    ALTER COLUMN total_score TYPE NUMERIC(6,2) USING total_score::NUMERIC;

ALTER TABLE review_scores
    ALTER COLUMN score TYPE NUMERIC(6,2) USING score::NUMERIC;

CREATE TABLE work_attachments (
    id UUID PRIMARY KEY,
    work_id UUID NOT NULL REFERENCES works(id) ON DELETE CASCADE,
    kind VARCHAR(50) NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    stored_path VARCHAR(1000) NOT NULL,
    mime_type VARCHAR(200),
    size_bytes BIGINT,
    is_voice BOOLEAN NOT NULL DEFAULT FALSE,
    duration_ms BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_work_attachments_work ON work_attachments(work_id);

CREATE TABLE review_attachments (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    kind VARCHAR(50) NOT NULL,
    original_filename VARCHAR(500),
    stored_path VARCHAR(1000),
    mime_type VARCHAR(200),
    size_bytes BIGINT,
    is_voice BOOLEAN NOT NULL DEFAULT FALSE,
    duration_ms BIGINT,
    external_url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_review_attachments_review ON review_attachments(review_id);
