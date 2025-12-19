-- 学习指南文章系统

-- 创建文章标签表
CREATE TABLE IF NOT EXISTS article_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    color VARCHAR(20) DEFAULT '#3B82F6',
    sort_order INTEGER DEFAULT 0,
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建文章表
CREATE TABLE IF NOT EXISTS article (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL REFERENCES sys_user(id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reviewer_id BIGINT REFERENCES sys_user(id),
    review_comment VARCHAR(500),
    review_time TIMESTAMP,
    publish_time TIMESTAMP,
    view_count INTEGER DEFAULT 0,
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建文章-标签关联表
CREATE TABLE IF NOT EXISTS article_tag_relation (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES article(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES article_tag(id) ON DELETE CASCADE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(article_id, tag_id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_article_author ON article(author_id);
CREATE INDEX IF NOT EXISTS idx_article_status ON article(status);
CREATE INDEX IF NOT EXISTS idx_article_publish_time ON article(publish_time DESC) WHERE status = 'PUBLISHED' AND deleted = 0;
CREATE INDEX IF NOT EXISTS idx_article_create_time ON article(create_time DESC);
CREATE INDEX IF NOT EXISTS idx_article_tag_relation_article ON article_tag_relation(article_id);
CREATE INDEX IF NOT EXISTS idx_article_tag_relation_tag ON article_tag_relation(tag_id);
CREATE INDEX IF NOT EXISTS idx_article_tag_name ON article_tag(name);
CREATE INDEX IF NOT EXISTS idx_article_tag_sort ON article_tag(sort_order);

-- 创建 update_time 触发器
DROP TRIGGER IF EXISTS trg_article_update_time ON article;
CREATE TRIGGER trg_article_update_time
    BEFORE UPDATE ON article
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

DROP TRIGGER IF EXISTS trg_article_tag_update_time ON article_tag;
CREATE TRIGGER trg_article_tag_update_time
    BEFORE UPDATE ON article_tag
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- 表注释
COMMENT ON TABLE article IS '学习指南文章表';
COMMENT ON TABLE article_tag IS '文章标签表';
COMMENT ON TABLE article_tag_relation IS '文章-标签关联表';
COMMENT ON COLUMN article.status IS '文章状态: DRAFT(草稿), PENDING(待审核), PUBLISHED(已发布), REJECTED(已拒绝)';
COMMENT ON COLUMN article.content IS 'Markdown 格式的文章内容';

-- 插入默认标签
INSERT INTO article_tag (name, description, color, sort_order)
VALUES
    ('入门指南', 'CTF 入门学习资料', '#22C55E', 1),
    ('Web安全', 'Web 安全相关教程', '#3B82F6', 2),
    ('逆向工程', '逆向分析技术', '#EF4444', 3),
    ('密码学', '密码学基础与进阶', '#F59E0B', 4),
    ('PWN', '二进制漏洞利用', '#8B5CF6', 5),
    ('杂项', '其他技术文章', '#6B7280', 6)
ON CONFLICT (name) DO NOTHING;
