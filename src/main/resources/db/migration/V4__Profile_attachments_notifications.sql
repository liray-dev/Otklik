ALTER TABLE users
    ADD COLUMN full_name VARCHAR(200),
    ADD COLUMN phone VARCHAR(50),
    ADD COLUMN telegram VARCHAR(200),
    ADD COLUMN avatar_path VARCHAR(500),
    ADD COLUMN about_me TEXT,
    ADD COLUMN university_group VARCHAR(100);

CREATE TABLE campaign_attachments (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    kind VARCHAR(50) NOT NULL,
    original_filename VARCHAR(500),
    stored_path VARCHAR(1000),
    mime_type VARCHAR(200),
    size_bytes BIGINT,
    external_url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_campaign_attachments_campaign ON campaign_attachments(campaign_id);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    message VARCHAR(500) NOT NULL,
    link VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read);

CREATE TEMP TABLE duplicate_works AS
SELECT id FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY student_id, campaign_id ORDER BY created_at DESC) AS rn
    FROM works
) t WHERE t.rn > 1;

DELETE FROM review_scores WHERE review_id IN (
    SELECT r.id FROM reviews r
    JOIN work_assignments wa ON r.assignment_id = wa.id
    WHERE wa.work_id IN (SELECT id FROM duplicate_works)
);

DELETE FROM review_attachments WHERE review_id IN (
    SELECT r.id FROM reviews r
    JOIN work_assignments wa ON r.assignment_id = wa.id
    WHERE wa.work_id IN (SELECT id FROM duplicate_works)
);

DELETE FROM reviews WHERE assignment_id IN (
    SELECT id FROM work_assignments WHERE work_id IN (SELECT id FROM duplicate_works)
);

DELETE FROM work_assignments WHERE work_id IN (SELECT id FROM duplicate_works);

DELETE FROM work_attachments WHERE work_id IN (SELECT id FROM duplicate_works);

DELETE FROM works WHERE id IN (SELECT id FROM duplicate_works);

DROP TABLE duplicate_works;

ALTER TABLE works
    ADD CONSTRAINT uk_works_student_campaign UNIQUE (student_id, campaign_id);
