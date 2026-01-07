-- Competition Status Tracking Enhancement
-- This migration adds support for automatic status updates and admin override

-- Add status_updated_at for auditing when status was last changed
ALTER TABLE competition ADD COLUMN IF NOT EXISTS status_updated_at TIMESTAMP;

-- Add status_override for admin manual control (NULL = use computed, non-NULL = use this value)
ALTER TABLE competition ADD COLUMN IF NOT EXISTS status_override VARCHAR(20);

-- Comments for new columns
COMMENT ON COLUMN competition.status_updated_at IS 'Timestamp when status was last updated (by scheduler or admin)';
COMMENT ON COLUMN competition.status_override IS 'Manual status override (NULL = use computed from times, non-NULL = use this value)';

-- Add index for scheduler queries (find competitions needing status updates)
CREATE INDEX IF NOT EXISTS idx_competition_status_time
    ON competition(status, start_time, end_time)
    WHERE deleted = 0;

-- Add partial indexes for efficient scheduler lookups
-- Index for finding inactive competitions ready to start
CREATE INDEX IF NOT EXISTS idx_competition_pending_start
    ON competition(start_time)
    WHERE status = 'inactive' AND status_override IS NULL AND deleted = 0;

-- Index for finding active competitions ready to end
CREATE INDEX IF NOT EXISTS idx_competition_pending_end
    ON competition(end_time)
    WHERE status = 'active' AND status_override IS NULL AND deleted = 0;

-- Initialize status_updated_at for existing records
UPDATE competition SET status_updated_at = update_time WHERE status_updated_at IS NULL;

-- ShedLock table for distributed scheduler locking
-- This ensures only one instance runs the scheduled task in a clustered deployment
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

COMMENT ON TABLE shedlock IS 'ShedLock table for distributed scheduler locking';
