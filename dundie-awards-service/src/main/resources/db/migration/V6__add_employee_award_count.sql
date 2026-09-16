ALTER TABLE employees ADD COLUMN award_count INTEGER NOT NULL DEFAULT 0;

-- the awards table stays the source of truth, so the counter is derived from it here
-- and can be rebuilt with this same statement if it ever drifts
UPDATE employees employee
SET award_count = (
    SELECT count(*)
    FROM dundie_awards award
    WHERE award.recipient_id = employee.id
);

-- matches the leaderboard's filter and ordering so paging is an index scan rather than
-- a sort over every employee
CREATE INDEX idx_employees_award_count ON employees (award_count DESC, id ASC)
    WHERE deleted_at IS NULL AND award_count > 0;
