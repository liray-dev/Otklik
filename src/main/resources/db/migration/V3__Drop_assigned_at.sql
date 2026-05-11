UPDATE work_assignments SET taken_at = assigned_at WHERE taken_at IS NULL;

ALTER TABLE work_assignments
    DROP COLUMN assigned_at;
