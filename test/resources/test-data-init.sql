-- ================================================
-- AI技术交流综合论坛 - 测试数据初始化脚本
-- ================================================
-- 说明：此脚本用于初始化测试数据库，包含基础测试数据
-- 使用前请先执行以下步骤：
-- 1. 创建测试数据库：CREATE DATABASE IF NOT EXISTS forum_test DEFAULT CHARACTER SET utf8mb4;
-- 2. 执行表结构脚本：mysql -u root -p forum_test < src/main/resources/db/schema.sql
-- 3. 执行本脚本：mysql -u root -p forum_test < test/resources/test-data-init.sql
-- ================================================

USE forum_test;

-- 清空现有测试数据（保留表结构）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_badges;
TRUNCATE TABLE badges;
TRUNCATE TABLE user_preferences;
TRUNCATE TABLE reports;
TRUNCATE TABLE drafts;
TRUNCATE TABLE notifications;
TRUNCATE TABLE bookmarks;
TRUNCATE TABLE reactions;
TRUNCATE TABLE topic_reads;
TRUNCATE TABLE topic_tags;
TRUNCATE TABLE posts;
TRUNCATE TABLE topics;
TRUNCATE TABLE tags;
TRUNCATE TABLE tag_groups;
TRUNCATE TABLE categories;
TRUNCATE TABLE user_roles;
TRUNCATE TABLE roles;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ================================================
-- 1. 插入测试角色数据
-- ================================================
INSERT INTO roles (id, code, name, description, created_at, updated_at) VALUES
(1, 'USER', '普通用户', '论坛普通用户角色', NOW(), NOW()),
(2, 'MODERATOR', '版主', '论坛版主，可以管理内容', NOW(), NOW()),
(3, 'ADMIN', '管理员', '系统管理员，拥有全部权限', NOW(), NOW());

-- ================================================
-- 2. 插入测试用户数据
-- ================================================
-- 密码均为：password123（BCrypt加密）
-- 注意：字段名匹配实际schema.sql中的定义
INSERT INTO users (id, username, email, password_hash, display_name, avatar_url, bio, location, website,
                   trust_level, role, status, topics_count, posts_count, likes_received_count,
                   last_seen_at, created_at, updated_at) VALUES
(1, 'admin', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 '系统管理员', '/images/avatars/admin.png', '系统管理员账号', '北京', 'https://example.com',
 4, 'ADMIN', 'ACTIVE', 10, 50, 100, NOW(), NOW(), NOW()),

(2, 'moderator', 'moderator@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 '版主', '/images/avatars/mod.png', '论坛版主', '上海', '',
 3, 'MODERATOR', 'ACTIVE', 5, 30, 50, NOW(), NOW(), NOW()),

(3, 'testuser1', 'testuser1@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 '测试用户1', '/images/avatars/user1.png', 'AI技术爱好者', '深圳', '',
 1, 'USER', 'ACTIVE', 3, 15, 20, NOW(), NOW(), NOW()),

(4, 'testuser2', 'testuser2@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 '测试用户2', '/images/avatars/user2.png', '机器学习研究者', '杭州', '',
 1, 'USER', 'ACTIVE', 2, 10, 15, NOW(), NOW(), NOW());

-- ================================================
-- 3. 分配用户角色
-- ================================================
INSERT INTO user_roles (user_id, role_id, created_at) VALUES
(1, 3, NOW()), -- admin是管理员
(2, 2, NOW()), -- moderator是版主
(3, 1, NOW()), -- testuser1是普通用户
(4, 1, NOW()); -- testuser2是普通用户

-- ================================================
-- 4. 插入测试分类数据
-- ================================================
-- 注意：字段名为 min_trust_level 不是 minimum_trust_level
INSERT INTO categories (id, parent_id, name, slug, description, color, text_color, icon, position,
                        min_trust_level, topic_count, post_count, created_at, updated_at) VALUES
(1, NULL, '大语言模型', 'llm', '讨论GPT、Claude、LLaMA等大语言模型', '#3498db', '#ffffff', 'robot', 1, 0, 5, 10, NOW(), NOW()),
(2, NULL, 'Agent开发', 'agent', '智能Agent开发与应用', '#e74c3c', '#ffffff', 'brain', 2, 0, 3, 8, NOW(), NOW()),
(3, NULL, 'RAG应用', 'rag', '检索增强生成技术讨论', '#2ecc71', '#ffffff', 'search', 3, 0, 2, 5, NOW(), NOW()),
(4, NULL, '模型部署', 'deployment', '模型部署与优化', '#f39c12', '#ffffff', 'server', 4, 0, 2, 6, NOW(), NOW()),
(5, NULL, '开源项目', 'opensource', 'AI开源项目分享', '#9b59b6', '#ffffff', 'github', 5, 0, 1, 3, NOW(), NOW());

-- ================================================
-- 5. 插入测试标签组数据
-- ================================================
INSERT INTO tag_groups (id, name, description, color, sort_order, created_at, updated_at) VALUES
(1, '技术栈', 'AI技术相关标签', '#3498db', 1, NOW(), NOW()),
(2, '应用场景', 'AI应用场景标签', '#2ecc71', 2, NOW(), NOW()),
(3, '难度等级', '内容难度分级', '#e74c3c', 3, NOW(), NOW());

-- ================================================
-- 6. 插入测试标签数据
-- ================================================
-- 注意：没有 updated_at 字段
INSERT INTO tags (id, name, slug, group_name, color, icon, topic_count, created_at) VALUES
(1, 'Python', 'python', '技术栈', '#3776ab', 'code', 5, NOW()),
(2, 'PyTorch', 'pytorch', '技术栈', '#ee4c2c', 'fire', 3, NOW()),
(3, 'TensorFlow', 'tensorflow', '技术栈', '#ff6f00', 'cube', 2, NOW()),
(4, '自然语言处理', 'nlp', '应用场景', '#16a085', 'message', 4, NOW()),
(5, '计算机视觉', 'cv', '应用场景', '#8e44ad', 'eye', 2, NOW()),
(6, '入门', 'beginner', '难度等级', '#27ae60', 'star', 3, NOW()),
(7, '进阶', 'advanced', '难度等级', '#f39c12', 'rocket', 2, NOW());

-- ================================================
-- 7. 插入测试话题数据
-- ================================================
-- 注意：字段名为 views_count, replies_count, likes_count, bookmarks_count, solution_post_id, last_post_at
INSERT INTO topics (id, user_id, category_id, title, slug, excerpt, views_count, replies_count,
                    likes_count, bookmarks_count, status, pinned, solved, solution_post_id,
                    last_post_id, last_post_at, created_at, updated_at) VALUES
(1, 1, 1, '如何使用Claude API进行文本生成？', 'claude-api-text-generation',
 '想了解Claude API的基本使用方法和最佳实践', 120, 5, 8, 3, 'NORMAL', 1, 1, 2, 6, NOW(), NOW(), NOW()),

(2, 3, 2, 'LangChain Agent开发入门教程', 'langchain-agent-tutorial',
 '分享LangChain Agent的开发经验', 85, 3, 5, 2, 'NORMAL', 0, 0, NULL, 4, NOW(), NOW(), NOW()),

(3, 4, 3, 'RAG系统中的向量数据库选择', 'rag-vector-database-selection',
 '对比不同向量数据库在RAG中的表现', 95, 4, 6, 4, 'NORMAL', 0, 0, NULL, 8, NOW(), NOW(), NOW());

-- ================================================
-- 8. 插入话题标签关联
-- ================================================
-- 注意：没有 created_at 字段
INSERT INTO topic_tags (topic_id, tag_id) VALUES
(1, 1), -- Python
(1, 4), -- NLP
(2, 1), -- Python
(2, 6), -- 入门
(3, 1), -- Python
(3, 7); -- 进阶

-- ================================================
-- 9. 插入测试回复数据
-- ================================================
-- 注意：字段名为 likes_count 不是 like_count
INSERT INTO posts (id, topic_id, user_id, reply_to_post_id, post_number, raw_content, cooked_content,
                   likes_count, status, created_at, updated_at) VALUES
(1, 1, 1, NULL, 1, '想了解Claude API的基本使用方法...', '<p>想了解Claude API的基本使用方法...</p>', 3, 'NORMAL', NOW(), NOW()),
(2, 1, 2, 1, 2, '你可以参考官方文档...', '<p>你可以参考官方文档...</p>', 5, 'NORMAL', NOW(), NOW()),
(3, 2, 3, NULL, 1, 'LangChain Agent开发教程...', '<p>LangChain Agent开发教程...</p>', 2, 'NORMAL', NOW(), NOW()),
(4, 2, 4, 3, 2, '感谢分享，很有帮助！', '<p>感谢分享，很有帮助！</p>', 1, 'NORMAL', NOW(), NOW()),
(5, 3, 4, NULL, 1, 'RAG向量数据库对比...', '<p>RAG向量数据库对比...</p>', 3, 'NORMAL', NOW(), NOW());

-- ================================================
-- 测试数据初始化完成
-- ================================================
-- 统计信息：
-- 用户数：4（1个管理员，1个版主，2个普通用户）
-- 角色数：3
-- 分类数：5
-- 标签组数：3
-- 标签数：7
-- 话题数：3
-- 回复数：5
-- ================================================
--
-- 执行完成后，测试数据库已准备就绪
-- 默认测试账号密码均为：password123
-- ================================================
