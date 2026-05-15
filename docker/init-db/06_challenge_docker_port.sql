-- 题目级 Docker 容器服务端口（未设置时使用 application.yml 中的 docker.container.container-port 默认值）
ALTER TABLE challenge
    ADD COLUMN IF NOT EXISTS docker_port INTEGER;

COMMENT ON COLUMN challenge.docker_port IS '动态容器题目对外服务端口；NULL 表示使用平台默认端口';
