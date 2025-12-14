# NKCTF 平台后端

一个由 NKCTF 团队使用 Java 构建的现代化 CTF（夺旗赛）竞赛平台后端系统。

[English](README.md)

## 功能特性

- **用户管理**：注册、登录、个人资料管理，基于 JWT 认证
- **题目管理**：多分类题目支持（Web、PWN、逆向、密码学、杂项）
- **比赛系统**：支持个人赛和团队赛
- **动态 Flag**：基于 Docker 容器隔离的用户独立 Flag
- **团队系统**：团队创建、邀请、队长管理
- **排行榜**：实时计分和排名
- **提示系统**：可解锁的提示，消耗积分
- **限流保护**：基于 Resilience4j 的 API 限流
- **API 文档**：自动生成的 Swagger/OpenAPI 文档

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 运行时（启用虚拟线程） |
| Spring Boot | 4.0.0 | Web 框架 |
| Spring Security | 6.x | 认证授权 |
| MyBatis-Plus | 3.5.15 | ORM 框架 |
| PostgreSQL | 16 | 主数据库 |
| Redis | 7 | 缓存、会话、分布式锁 |
| Docker Java | 3.4.0 | 容器管理 |
| JJWT | 0.12.6 | JWT Token 处理 |

## 环境要求

- JDK 21+
- Docker & Docker Compose
- Maven 3.9+（或使用内置的 `mvnw`）

## 快速开始

### 1. 启动依赖服务

```bash
cd docker && docker compose up -d
```

这将启动 PostgreSQL 和 Redis 容器。

### 2. 运行应用

```bash
./mvnw spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

### 3. 访问 API 文档

访问：`http://localhost:8080/api/swagger-ui.html`

## 项目结构

```
src/main/java/cn/edu/ndky/nkctf/
├── config/          # 配置类
├── controller/      # REST 控制器
├── service/         # 服务接口
│   └── impl/        # 服务实现
├── mapper/          # MyBatis Mapper 接口
├── entity/          # 数据库实体
├── dto/             # 数据传输对象
│   ├── request/     # 请求 DTO
│   └── response/    # 响应 DTO
├── security/        # 安全相关组件
├── util/            # 工具类
├── exception/       # 异常定义
└── NkctfApplication.java

src/main/resources/
├── application.yml  # 主配置文件
└── mapper/          # MyBatis XML 映射文件

docker/
├── docker-compose.yml
└── init-db/         # 数据库初始化脚本
```

## 开发指南

### 常用命令

```bash
# 编译项目
./mvnw compile

# 运行测试
./mvnw test

# 打包（跳过测试）
./mvnw package -DskipTests

# 清理构建
./mvnw clean
```

### 数据库连接

```
PostgreSQL: localhost:5432/nkctf
  用户名: nkctf
  密码: nkctf123456

Redis: localhost:6379
  密码: nkctf123456
```

## API 概览

| 接口 | 描述 |
|------|------|
| `POST /api/auth/register` | 用户注册 |
| `POST /api/auth/login` | 用户登录 |
| `GET /api/challenges` | 获取题目列表 |
| `POST /api/challenges/{id}/submit` | 提交 Flag |
| `GET /api/competitions` | 获取比赛列表 |
| `GET /api/leaderboard` | 获取排行榜 |
| `GET /api/teams` | 团队管理 |

## 配置说明

`application.yml` 中的关键配置：

```yaml
jwt:
  secret: your-secret-key  # 生产环境必须修改！
  expiration: 604800000    # 7 天

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nkctf
  redis:
    host: localhost
    port: 6379
```

## 安全注意事项

1. ==**生产环境部署必须修改**==：
   - JWT 密钥
   - 数据库密码
   - Redis 密码

2. **敏感信息不要提交**到代码仓库

3. **容器资源限制**：配置内存和 CPU 限制防止资源耗尽

4. **输入校验**：所有用户输入必须校验，防止注入攻击

## 许可证

MIT License

## 贡献指南

欢迎贡献代码！
