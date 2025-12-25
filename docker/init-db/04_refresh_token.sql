-- Refresh Token 表 (用于双令牌认证机制)
CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    user_agent VARCHAR(500),
    ip_address VARCHAR(45),
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_token(token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires ON refresh_token(expires_at);

-- 表注释
COMMENT ON TABLE refresh_token IS 'Refresh Token 表 - 用于双令牌认证';
COMMENT ON COLUMN refresh_token.token IS '高熵随机值 (UUID v4)';
COMMENT ON COLUMN refresh_token.user_id IS '关联用户ID';
COMMENT ON COLUMN refresh_token.user_agent IS '客户端 User-Agent';
COMMENT ON COLUMN refresh_token.ip_address IS '客户端 IP 地址';
COMMENT ON COLUMN refresh_token.expires_at IS '过期时间';
COMMENT ON COLUMN refresh_token.revoked IS '是否已撤销';
