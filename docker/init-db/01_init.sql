-- 创建竞赛表
CREATE TABLE IF NOT EXISTS competition (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_team_competition BOOLEAN DEFAULT TRUE, -- TRUE: 团队赛, FALSE: 个人赛
    status VARCHAR(20) DEFAULT 'inactive', -- inactive, active, ending
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建队伍表 (使用 UUID 作为主键)
CREATE TABLE IF NOT EXISTS team (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    invite_token UUID DEFAULT gen_random_uuid(), -- 邀请码，可刷新
    captain_id BIGINT,
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    bio TEXT,
    role VARCHAR(20) DEFAULT 'USER',
    enabled BOOLEAN DEFAULT TRUE,
    score INTEGER DEFAULT 0,
    solved_count INTEGER DEFAULT 0,
    last_submit_time TIMESTAMP,
    team_id UUID REFERENCES team(id),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 添加队伍表的队长外键 (需要在 sys_user 创建后添加)
ALTER TABLE team ADD CONSTRAINT fk_team_captain FOREIGN KEY (captain_id) REFERENCES sys_user(id);

-- 创建用户队伍关联表 (用于竞赛队伍)
CREATE TABLE IF NOT EXISTS team_member (
    id BIGSERIAL PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES team(id),
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    role VARCHAR(20) DEFAULT 'MEMBER', -- CAPTAIN, MEMBER
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(team_id, user_id)
);

-- 创建题目表
CREATE TABLE IF NOT EXISTS challenge (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    content TEXT,
    category VARCHAR(50) NOT NULL,
    difficulty VARCHAR(20) DEFAULT 'MEDIUM',
    points INTEGER DEFAULT 100,
    author VARCHAR(100),
    flag VARCHAR(255),
    is_dynamic BOOLEAN DEFAULT FALSE,
    docker_image VARCHAR(255),
    attachment_url VARCHAR(500),
    attachment_name VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE,
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建队伍参赛表 (团队赛)
CREATE TABLE IF NOT EXISTS competition_team (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL REFERENCES competition(id),
    team_id UUID NOT NULL REFERENCES team(id),
    score INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(competition_id, team_id)
);

-- 创建个人参赛表 (个人赛)
CREATE TABLE IF NOT EXISTS competition_user (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL REFERENCES competition(id),
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    score INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(competition_id, user_id)
);

-- 创建竞赛题目表
CREATE TABLE IF NOT EXISTS competition_challenge (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL REFERENCES competition(id),
    challenge_id BIGINT NOT NULL REFERENCES challenge(id),
    sort_order INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(competition_id, challenge_id)
);

-- 创建解题记录表
CREATE TABLE IF NOT EXISTS submission (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    challenge_id BIGINT NOT NULL REFERENCES challenge(id),
    competition_id BIGINT REFERENCES competition(id), -- 竞赛提交时关联竞赛
    team_id UUID REFERENCES team(id), -- 竞赛提交时关联队伍
    flag VARCHAR(255) NOT NULL,
    is_correct BOOLEAN DEFAULT FALSE,
    points_awarded INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建题目提示表
CREATE TABLE IF NOT EXISTS hint (
    id BIGSERIAL PRIMARY KEY,
    challenge_id BIGINT NOT NULL REFERENCES challenge(id),
    content TEXT NOT NULL,
    cost INTEGER DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建提示获取记录表
CREATE TABLE IF NOT EXISTS hint_unlock (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    hint_id BIGINT NOT NULL REFERENCES hint(id),
    cost INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, hint_id)
);

-- 防止同一用户对同一题目重复获得正确计分（部分唯一索引，非竞赛提交）
CREATE UNIQUE INDEX IF NOT EXISTS idx_submission_correct_unique
    ON submission(user_id, challenge_id) WHERE is_correct = TRUE AND competition_id IS NULL;

-- 防止同一队伍在同一竞赛中对同一题目重复获得正确计分
CREATE UNIQUE INDEX IF NOT EXISTS idx_submission_competition_correct_unique
    ON submission(team_id, competition_id, challenge_id) WHERE is_correct = TRUE AND competition_id IS NOT NULL;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_user_username ON sys_user(username);
CREATE INDEX IF NOT EXISTS idx_user_email ON sys_user(email);
CREATE INDEX IF NOT EXISTS idx_user_team ON sys_user(team_id);
CREATE INDEX IF NOT EXISTS idx_user_leaderboard ON sys_user(score DESC, solved_count ASC, last_submit_time ASC) WHERE deleted = 0 AND enabled = TRUE;
CREATE INDEX IF NOT EXISTS idx_submission_user ON submission(user_id);
CREATE INDEX IF NOT EXISTS idx_submission_challenge ON submission(challenge_id);
CREATE INDEX IF NOT EXISTS idx_submission_competition ON submission(competition_id);
CREATE INDEX IF NOT EXISTS idx_submission_time ON submission(create_time);
CREATE INDEX IF NOT EXISTS idx_challenge_category ON challenge(category);
CREATE INDEX IF NOT EXISTS idx_hint_challenge ON hint(challenge_id);
CREATE INDEX IF NOT EXISTS idx_hint_unlock_user ON hint_unlock(user_id);
CREATE INDEX IF NOT EXISTS idx_hint_unlock_hint ON hint_unlock(hint_id);
CREATE INDEX IF NOT EXISTS idx_competition_status ON competition(status);
CREATE INDEX IF NOT EXISTS idx_team_member_team ON team_member(team_id);
CREATE INDEX IF NOT EXISTS idx_team_member_user ON team_member(user_id);
CREATE INDEX IF NOT EXISTS idx_competition_team_competition ON competition_team(competition_id);
CREATE INDEX IF NOT EXISTS idx_competition_team_team ON competition_team(team_id);
CREATE INDEX IF NOT EXISTS idx_competition_user_competition ON competition_user(competition_id);
CREATE INDEX IF NOT EXISTS idx_competition_user_user ON competition_user(user_id);
CREATE INDEX IF NOT EXISTS idx_competition_challenge_competition ON competition_challenge(competition_id);

-- 创建 update_time 自动更新函数
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建 update_time 触发器
DROP TRIGGER IF EXISTS trg_user_update_time ON sys_user;
CREATE TRIGGER trg_user_update_time
    BEFORE UPDATE ON sys_user
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

DROP TRIGGER IF EXISTS trg_team_update_time ON team;
CREATE TRIGGER trg_team_update_time
    BEFORE UPDATE ON team
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

DROP TRIGGER IF EXISTS trg_challenge_update_time ON challenge;
CREATE TRIGGER trg_challenge_update_time
    BEFORE UPDATE ON challenge
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

DROP TRIGGER IF EXISTS trg_hint_update_time ON hint;
CREATE TRIGGER trg_hint_update_time
    BEFORE UPDATE ON hint
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

DROP TRIGGER IF EXISTS trg_competition_update_time ON competition;
CREATE TRIGGER trg_competition_update_time
    BEFORE UPDATE ON competition
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- 插入管理员用户 (密码: admin123)
-- BCrypt hash 由 Spring Security BCryptPasswordEncoder 生成
INSERT INTO sys_user (username, password, email, nickname, role, enabled, score)
VALUES ('admin', '$2a$10$.83ub7O9jB0wD8jIrwQ1Lel.jPiAf/q6GMkNOkb8NRSVY4/JdItSe', 'admin@nkctf.com', '管理员', 'ADMIN', TRUE, 0)
ON CONFLICT (username) DO NOTHING;

-- 表注释
COMMENT ON TABLE competition IS '竞赛表';
COMMENT ON TABLE team IS '队伍表';
COMMENT ON TABLE team_member IS '用户队伍关联表';
COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON TABLE challenge IS '题目表';
COMMENT ON TABLE competition_team IS '队伍参赛表 (团队赛)';
COMMENT ON TABLE competition_user IS '用户参赛表 (个人赛)';
COMMENT ON TABLE competition_challenge IS '竞赛题目表';
COMMENT ON TABLE submission IS '解题记录表';
COMMENT ON TABLE hint IS '题目提示表';
COMMENT ON TABLE hint_unlock IS '提示获取记录表';

-- 插入密码学 CTF 题目 (练习题目，enabled=TRUE)
INSERT INTO challenge (title, description, content, category, difficulty, points, author, flag, is_dynamic, enabled, attachment_url, attachment_name)
VALUES
(
    'Base64 入门',
    '最基础的编码方式，这不是加密！',
    E'小明在学习网络安全时，发现了一串奇怪的字符：\n\n```\nbmtjdGZ7YjRzM182NF8xc19uMHRfM25jcnlwdDEwbn0=\n```\n\n这看起来像是某种编码，你能帮他解码吗？',
    'CRYPTO',
    'EASY',
    50,
    'admin',
    'nkctf{b4s3_64_1s_n0t_3ncrypt10n}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=crypto/base64_challenge.txt',
    'base64_challenge.txt'
),
(
    '凯撒大帝的密信',
    '古罗马的经典密码术',
    E'据说凯撒大帝在战争中使用一种简单的密码来传递军事情报。\n\n现在你截获了一封密信：\n\n```\nqnfwi{f43v4u_f1sk3u_1v_34vb}\n```\n\n已知凯撒使用的偏移量是 3，你能破解这封密信吗？',
    'CRYPTO',
    'EASY',
    100,
    'admin',
    'nkctf{c43s4r_c1ph3r_1s_34sy}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=crypto/caesar_challenge.txt',
    'caesar_challenge.txt'
),
(
    'ROT13 轮转',
    '一种特殊的凯撒密码',
    E'ROT13 是一种简单的替换密码，它将字母表分成两半并相互替换。\n\n你收到了这样一条消息：\n\n```\naxpgs{e0g_gu1eg33a_e0gng10a}\n```',
    'CRYPTO',
    'EASY',
    100,
    'admin',
    'nkctf{r0t_th1rt33n_r0tat10n}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=crypto/rot13_challenge.txt',
    'rot13_challenge.txt'
),
(
    '十六进制世界',
    '计算机的基础编码',
    E'在计算机世界中，十六进制是一种常用的数据表示方式。\n\n请解码以下十六进制字符串：\n\n```\n6e6b6374667b6833785f6433633064316e675f66756e7d\n```',
    'CRYPTO',
    'EASY',
    50,
    'admin',
    'nkctf{h3x_d3c0d1ng_fun}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=crypto/hex_challenge.txt',
    'hex_challenge.txt'
),
(
    '异或之美',
    '可逆运算的魅力',
    E'异或 (XOR) 是密码学中最基本的操作之一，它有一个神奇的特性：A XOR B XOR B = A\n\n以下是一段被单字节密钥异或加密的十六进制数据：\n\n```\n3b3e3037331a0d300c3438363c300b38363f303c\n```',
    'CRYPTO',
    'MEDIUM',
    150,
    'admin',
    'nkctf{x0r_1s_r3v3rs1bl3}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=crypto/xor_challenge.txt',
    'xor_challenge.txt'
),
(
    'Simple Check',
    'Check~So simple~',
    E'Find the simple flag in this challenge.\n\nFlag format: flag{...}',
    'REVERSE',
    'EASY',
    100,
    'admin',
    'flag{MAth_i&_GOOd_DON7_90V_7hInK?}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=re/simplecheck.apk',
    'simplcheck.apk'
),
(
    'DDCTF-Easy-apk',
    'DDCTF-Easy-apk',
    E'This is a simple reverse engineering challenge.\n\nFlag format: nkctf{...}',
    'REVERSE',
    'EASY',
    100,
    'admin',
    'nkctf{DDCTF-3ad60811d87c4a2dba0ef651b2d93476@didichuxing.com}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=re/DDCTF-Easy.apk.64812266499cc050ac23e190e53b87f7.zip',
    'DDCTF-Easy.apk.64812266499cc050ac23e190e53b87f7.zip'
),
(
    'Smali:Crackme',
    'easy smali',
    E'This is a simple smali reverse engineering challenge.\n\nFlag format: PCTF{...}',
    'REVERSE',
    'EASY',
    100,
    'admin',
    'PCTF{Sm4liRiver}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=re/Crackme.smali',
    'Crackme.smali'
),
(
    'CCF 100',
    '爬楼梯',
    E'爬得够高才能看到更远的风景。flag 格式: nkctf{...}',
    'REVERSE',
    'EASY',
    100,
    'admin',
    'nkctf{268796A5E68A25A1}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=re/CFF_100.apk',
    'CFF_100.apk'
),
(
    'MD5',
    'MD5',
    E'Flag format: nkctf{...}',
    'CRYPTO',
    'EASY',
    10,
    'admin',
    'nkctf{admin1}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=crypto/27120bd8-e273-4528-97a9-28dcebe236de.zip',
    '27120bd8-e273-4528-97a9-28dcebe236de.zip'
),
(
    'URL Encode',
    'url encode',
    E'Flag format: flag{...}',
    'CRYPTO',
    'EASY',
    10,
    'admin',
    'flag{and 1=1}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=crypto/8c1b8065-c3ae-46d7-afc7-1be8d95aac47.zip',
    '8c1b8065-c3ae-46d7-afc7-1be8d95aac47.zip'
),
(
    '看我回旋踢',
    '终于给你头踢正了...',
    E'Flag format: flag{...}',
    'CRYPTO',
    'EASY',
    10,
    'admin',
    'flag{5cd1004d-86a5-46d8-b720-beb5ba0417e1}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=crypto/1784aa2b-cfcb-428e-949c-d6e4728fb94f.zip',
    '1784aa2b-cfcb-428e-949c-d6e4728fb94f.zip'
),
(
    'morse',
    'morse',
    E'Flag format: flag{...}',
    'CRYPTO',
    'EASY',
    10,
    'admin',
    'flag{ILOVEYOU}',
    FALSE,
    TRUE,
    '/api/attachments/download?path=crypto/1bb81ad9-8df8-49f1-bdae-0226869b16c8.zip',
    '1bb81ad9-8df8-49f1-bdae-0226869b16c8.zip'
)
ON CONFLICT DO NOTHING;

-- 插入动态容器题目 (Web)
INSERT INTO challenge (title, description, content, category, difficulty, points, author, flag, is_dynamic, docker_image, enabled)
VALUES
(
    'ThinkPHP RCE',
    'ThinkPHP 5.0.20 远程代码执行漏洞',
    E'## 题目描述\n\n这是一个运行 ThinkPHP 5.0.20 的 Web 应用。\n\n众所周知，ThinkPHP 5.x 版本存在远程代码执行漏洞（CVE-2018-20062），你能利用这个漏洞获取 Flag 吗？\n\n## 提示\n\n- Flag 存储在环境变量中\n- 漏洞与路由解析相关\n- 查阅 ThinkPHP 5.0.x RCE 漏洞分析文章\n\n## Flag 格式\n\n`nkctf{...}`（动态 Flag，每个用户不同）',
    'WEB',
    'MEDIUM',
    200,
    'admin',
    NULL,  -- 动态 Flag，由容器服务生成
    TRUE,  -- 标记为动态题目
    'vulhub/thinkphp:5.0.20',  -- Docker 镜像
    TRUE
),
(
    'PHP 文件包含',
    '经典的 LFI 漏洞利用',
    E'## 题目描述\n\n这是一个存在本地文件包含（LFI）漏洞的 PHP 应用。\n\n你能通过文件包含漏洞读取服务器上的敏感信息吗？\n\n## 目标\n\n读取 Flag（存储在环境变量中）\n\n## Flag 格式\n\n`nkctf{...}`（动态 Flag）',
    'WEB',
    'EASY',
    150,
    'admin',
    NULL,
    TRUE,
    'vulhub/php:5.4.45-apache-lfi',
    TRUE
)
ON CONFLICT DO NOTHING;

-- 插入动态容器题目的提示
INSERT INTO hint (challenge_id, content, cost, sort_order)
SELECT c.id, '尝试访问 /index.php?s=/index/\\think\\app/invokefunction&function=call_user_func_array', 50, 1
FROM challenge c WHERE c.title = 'ThinkPHP RCE'
ON CONFLICT DO NOTHING;

INSERT INTO hint (challenge_id, content, cost, sort_order)
SELECT c.id, '使用 phpinfo() 查看环境变量中的 FLAG', 50, 2
FROM challenge c WHERE c.title = 'ThinkPHP RCE'
ON CONFLICT DO NOTHING;

-- 插入题目提示
INSERT INTO hint (challenge_id, content, cost, sort_order)
VALUES
-- Base64 入门 (challenge_id = 1)
(1, '注意字符串末尾的等号，这是 Base64 编码的特征。', 10, 1),

-- 凯撒大帝的密信 (challenge_id = 2)
(2, '字母表向后移动 3 位，a→d, b→e, ...', 20, 1),
(2, '解密时需要将字母向前移动 3 位。', 20, 2),

-- ROT13 轮转 (challenge_id = 3)
(3, 'ROT13 的特点是加密和解密使用同样的操作。', 20, 1),
(3, '偏移量是 13，正好是字母表的一半。', 20, 2),

-- 十六进制世界 (challenge_id = 4)
(4, '每两个十六进制字符代表一个 ASCII 字符。', 10, 1),
(4, '可以使用 Python 的 bytes.fromhex() 函数。', 15, 2),

-- 异或之美 (challenge_id = 5)
(5, '密钥是一个单字节（0x00-0xFF）。', 30, 1),
(5, '明文以 "nkctf" 开头，可以用它来推算密钥。', 30, 2),
(5, '密钥 = 密文第一个字节 XOR 明文第一个字符的 ASCII 值。', 40, 3)
ON CONFLICT DO NOTHING;

-- 插入示例竞赛 (inactive 状态)
INSERT INTO competition (name, description, is_team_competition, status, start_time, end_time)
VALUES (
    'NKCTF 2025 新生赛',
    '面向新生的 CTF 入门竞赛，包含 Web、Crypto、Misc 等多个方向的基础题目。',
    TRUE,
    'inactive',
    '2025-01-01 09:00:00',
    '2025-01-03 18:00:00'
)
ON CONFLICT (name) DO NOTHING;
