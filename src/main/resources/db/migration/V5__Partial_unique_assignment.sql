ALTER TABLE work_assignments DROP CONSTRAINT IF EXISTS uk_work_reviewer;

CREATE UNIQUE INDEX uk_work_reviewer_active
    ON work_assignments(work_id, reviewer_id)
    WHERE status <> 'ABANDONED';
