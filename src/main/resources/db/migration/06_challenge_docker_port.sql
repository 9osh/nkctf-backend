-- 增量迁移：题目级 Docker 容器端口（幂等，应用每次启动时由 spring.sql.init 执行）
ALTER TABLE challenge
    ADD COLUMN IF NOT EXISTS docker_port INTEGER;

COMMENT ON COLUMN challenge.docker_port IS '动态容器题目对外服务端口；NULL 表示使用平台默认端口';
