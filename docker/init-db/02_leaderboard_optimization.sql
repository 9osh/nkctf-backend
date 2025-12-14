-- ============================================
-- 排行榜优化：添加用户冗余字段
-- 执行时间：2024-12-14
-- ============================================

-- 1. 添加冗余字段
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS solved_count INTEGER DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS last_submit_time TIMESTAMP;

-- 2. 为排行榜查询创建复合索引
CREATE INDEX IF NOT EXISTS idx_user_leaderboard
    ON sys_user(score DESC, solved_count ASC, last_submit_time ASC)
    WHERE deleted = 0 AND enabled = TRUE;

-- 3. 从现有数据初始化冗余字段
UPDATE sys_user u SET
    solved_count = COALESCE((
        SELECT COUNT(DISTINCT challenge_id)
        FROM submission s
        WHERE s.user_id = u.id AND s.is_correct = TRUE
    ), 0),
    last_submit_time = (
        SELECT MAX(create_time)
        FROM submission s
        WHERE s.user_id = u.id AND s.is_correct = TRUE
    );

-- 4. 添加字段注释
COMMENT ON COLUMN sys_user.solved_count IS '解题数量（冗余字段，用于排行榜优化）';
COMMENT ON COLUMN sys_user.last_submit_time IS '最后正确提交时间（冗余字段，用于排行榜优化）';
