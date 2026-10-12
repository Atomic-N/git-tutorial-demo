CREATE DATABASE IF NOT EXISTS ai_tef
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_tef;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(40) NOT NULL UNIQUE,
  email VARCHAR(120) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(60) NOT NULL,
  avatar_url VARCHAR(255) DEFAULT NULL,
  bio VARCHAR(1000) DEFAULT '',
  location VARCHAR(80) DEFAULT '',
  website VARCHAR(160) DEFAULT '',
  trust_level TINYINT NOT NULL DEFAULT 0,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  topics_count INT NOT NULL DEFAULT 0,
  posts_count INT NOT NULL DEFAULT 0,
  likes_received_count INT NOT NULL DEFAULT 0,
  last_seen_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_users_created_at (created_at),
  INDEX idx_users_trust_level (trust_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT DEFAULT NULL,
  name VARCHAR(60) NOT NULL,
  slug VARCHAR(80) NOT NULL UNIQUE,
  description VARCHAR(500) NOT NULL DEFAULT '',
  color VARCHAR(16) NOT NULL DEFAULT '#0ea5e9',
  text_color VARCHAR(16) NOT NULL DEFAULT '#111827',
  icon VARCHAR(40) NOT NULL DEFAULT 'message-square',
  position INT NOT NULL DEFAULT 0,
  min_trust_level TINYINT NOT NULL DEFAULT 0,
  topic_count INT NOT NULL DEFAULT 0,
  post_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL,
  INDEX idx_categories_position (position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(40) NOT NULL UNIQUE,
  slug VARCHAR(60) NOT NULL UNIQUE,
  tag_group_id BIGINT DEFAULT NULL,
  group_name VARCHAR(60) NOT NULL DEFAULT '常用标签',
  color VARCHAR(16) NOT NULL DEFAULT '#64748b',
  icon VARCHAR(40) NOT NULL DEFAULT 'hash',
  topic_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_tags_group_name (group_name),
  INDEX idx_tags_topic_count (topic_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(60) NOT NULL,
  description VARCHAR(255) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS category_moderators (
  category_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (category_id, user_id),
  CONSTRAINT fk_category_moderators_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
  CONSTRAINT fk_category_moderators_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_category_moderators_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tag_groups (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(60) NOT NULL UNIQUE,
  description VARCHAR(255) NOT NULL DEFAULT '',
  color VARCHAR(16) NOT NULL DEFAULT '#64748b',
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS topics (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  title VARCHAR(180) NOT NULL,
  slug VARCHAR(220) NOT NULL,
  excerpt VARCHAR(500) NOT NULL DEFAULT '',
  views_count INT NOT NULL DEFAULT 0,
  replies_count INT NOT NULL DEFAULT 0,
  likes_count INT NOT NULL DEFAULT 0,
  bookmarks_count INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  pinned BOOLEAN NOT NULL DEFAULT FALSE,
  solved BOOLEAN NOT NULL DEFAULT FALSE,
  solution_post_id BIGINT DEFAULT NULL,
  last_post_id BIGINT DEFAULT NULL,
  last_post_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_topics_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_topics_category FOREIGN KEY (category_id) REFERENCES categories(id),
  INDEX idx_topics_category_last_post (category_id, last_post_at),
  INDEX idx_topics_user_created (user_id, created_at),
  INDEX idx_topics_last_post_at (last_post_at),
  INDEX idx_topics_created_at (created_at),
  FULLTEXT KEY ft_topics_title_excerpt (title, excerpt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS posts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  topic_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  reply_to_post_id BIGINT DEFAULT NULL,
  post_number INT NOT NULL,
  raw_content TEXT NOT NULL,
  cooked_content MEDIUMTEXT NOT NULL,
  likes_count INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_posts_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE,
  CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_posts_reply_to FOREIGN KEY (reply_to_post_id) REFERENCES posts(id) ON DELETE SET NULL,
  UNIQUE KEY uk_posts_topic_number (topic_id, post_number),
  INDEX idx_posts_topic_created (topic_id, created_at),
  INDEX idx_posts_user_created (user_id, created_at),
  FULLTEXT KEY ft_posts_content (raw_content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS topic_tags (
  topic_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY (topic_id, tag_id),
  CONSTRAINT fk_topic_tags_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE,
  CONSTRAINT fk_topic_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS topic_reads (
  user_id BIGINT NOT NULL,
  topic_id BIGINT NOT NULL,
  last_read_post_number INT NOT NULL DEFAULT 0,
  last_read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, topic_id),
  CONSTRAINT fk_topic_reads_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_topic_reads_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  reaction_type VARCHAR(20) NOT NULL DEFAULT 'LIKE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uk_reactions_user_target (user_id, target_type, target_id),
  INDEX idx_reactions_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bookmarks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  note VARCHAR(255) DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_bookmarks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uk_bookmarks_user_target (user_id, target_type, target_id),
  INDEX idx_bookmarks_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  actor_id BIGINT DEFAULT NULL,
  type VARCHAR(40) NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  title VARCHAR(180) NOT NULL,
  body VARCHAR(500) DEFAULT '',
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_notifications_user_read_created (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS drafts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  draft_type VARCHAR(20) NOT NULL,
  target_id BIGINT DEFAULT NULL,
  title VARCHAR(180) DEFAULT '',
  content TEXT NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_drafts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_drafts_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  reason VARCHAR(80) NOT NULL,
  detail VARCHAR(1000) DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
  INDEX idx_reports_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS user_preferences (
  user_id BIGINT PRIMARY KEY,
  notify_on_reply BOOLEAN NOT NULL DEFAULT TRUE,
  notify_on_like BOOLEAN NOT NULL DEFAULT TRUE,
  email_digest BOOLEAN NOT NULL DEFAULT FALSE,
  email_on_mention BOOLEAN NOT NULL DEFAULT TRUE,
  theme VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
  editor_mode VARCHAR(20) NOT NULL DEFAULT 'RICH',
  auto_save_drafts BOOLEAN NOT NULL DEFAULT TRUE,
  hide_read_topics BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS badges (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(60) NOT NULL,
  description VARCHAR(255) NOT NULL DEFAULT '',
  icon VARCHAR(40) NOT NULL DEFAULT 'award',
  color VARCHAR(16) NOT NULL DEFAULT '#0ea5e9',
  sort_order INT NOT NULL DEFAULT 0,
  rule_type VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
  rule_threshold INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_badges_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_badges (
  user_id BIGINT NOT NULL,
  badge_id BIGINT NOT NULL,
  awarded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, badge_id),
  CONSTRAINT fk_user_badges_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_badges_badge FOREIGN KEY (badge_id) REFERENCES badges(id) ON DELETE CASCADE,
  INDEX idx_user_badges_awarded (awarded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS uploaded_files (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  original_name VARCHAR(180) NOT NULL,
  stored_name VARCHAR(120) NOT NULL UNIQUE,
  content_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  public_url VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_uploaded_files_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_uploaded_files_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_channels (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(60) NOT NULL,
  slug VARCHAR(80) NOT NULL UNIQUE,
  description VARCHAR(255) NOT NULL DEFAULT '',
  icon VARCHAR(40) NOT NULL DEFAULT 'messages-square',
  position INT NOT NULL DEFAULT 0,
  min_trust_level TINYINT NOT NULL DEFAULT 0,
  message_count INT NOT NULL DEFAULT 0,
  last_message_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  channel_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  reply_to_message_id BIGINT DEFAULT NULL,
  raw_content TEXT NOT NULL,
  cooked_content MEDIUMTEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_chat_messages_channel FOREIGN KEY (channel_id) REFERENCES chat_channels(id) ON DELETE CASCADE,
  CONSTRAINT fk_chat_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_chat_messages_reply FOREIGN KEY (reply_to_message_id) REFERENCES chat_messages(id) ON DELETE SET NULL,
  INDEX idx_chat_messages_channel_created (channel_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE tags
  ADD CONSTRAINT fk_tags_group FOREIGN KEY (tag_group_id) REFERENCES tag_groups(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS user_relationships (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_user_id BIGINT NOT NULL,
  relationship_type VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_relationships_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_relationships_target FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uk_user_relationships (user_id, target_user_id, relationship_type),
  INDEX idx_user_relationships_target_type (target_user_id, relationship_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS direct_message_threads (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  subject VARCHAR(180) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS direct_message_participants (
  thread_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  last_read_at DATETIME DEFAULT NULL,
  PRIMARY KEY (thread_id, user_id),
  CONSTRAINT fk_dm_participants_thread FOREIGN KEY (thread_id) REFERENCES direct_message_threads(id) ON DELETE CASCADE,
  CONSTRAINT fk_dm_participants_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_dm_participants_user (user_id, last_read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS direct_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  thread_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  body_raw TEXT NOT NULL,
  body_cooked MEDIUMTEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  quoted_message_id BIGINT DEFAULT NULL,
  message_status VARCHAR(20) NOT NULL DEFAULT 'SENT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delivered_at DATETIME DEFAULT NULL,
  read_at DATETIME DEFAULT NULL,
  CONSTRAINT fk_direct_messages_thread FOREIGN KEY (thread_id) REFERENCES direct_message_threads(id) ON DELETE CASCADE,
  CONSTRAINT fk_direct_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_dm_quoted_message FOREIGN KEY (quoted_message_id) REFERENCES direct_messages(id) ON DELETE SET NULL,
  INDEX idx_direct_messages_thread_created (thread_id, created_at),
  INDEX idx_dm_status_created (thread_id, message_status, created_at),
  INDEX idx_dm_quoted (quoted_message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS direct_message_thread_meta (
  thread_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
  is_archived BOOLEAN NOT NULL DEFAULT FALSE,
  is_muted BOOLEAN NOT NULL DEFAULT FALSE,
  pinned_at DATETIME DEFAULT NULL,
  archived_at DATETIME DEFAULT NULL,
  muted_at DATETIME DEFAULT NULL,
  PRIMARY KEY (thread_id, user_id),
  CONSTRAINT fk_dm_meta_thread FOREIGN KEY (thread_id) REFERENCES direct_message_threads(id) ON DELETE CASCADE,
  CONSTRAINT fk_dm_meta_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_dm_meta_user_pinned (user_id, is_pinned, pinned_at),
  INDEX idx_dm_meta_user_archived (user_id, is_archived, archived_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS site_settings (
  setting_key VARCHAR(80) PRIMARY KEY,
  setting_value TEXT NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_id BIGINT DEFAULT NULL,
  action VARCHAR(80) NOT NULL,
  target_type VARCHAR(40) NOT NULL,
  target_id BIGINT DEFAULT NULL,
  detail VARCHAR(500) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_audit_logs_created (created_at),
  INDEX idx_audit_logs_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO badges (code, name, description, icon, color, sort_order, rule_type, rule_threshold)
VALUES
  ('WELCOME', '社区居民', '完成注册并加入 AI TEF。', 'sparkles', '#0ea5e9', 10, 'REGISTRATION', 0),
  ('FIRST_TOPIC', '首发话题', '发布了第一个话题。', 'message-square-plus', '#16a34a', 20, 'TOPICS', 1),
  ('FIRST_REPLY', '开始交流', '发布了第一条回复。', 'reply', '#22c55e', 30, 'REPLIES', 1),
  ('TOPIC_BUILDER', '话题建设者', '累计发布 5 个话题。', 'blocks', '#14b8a6', 35, 'TOPICS', 5),
  ('ACTIVE_MEMBER', '活跃成员', '累计发布 10 条内容。', 'flame', '#f97316', 38, 'POSTS', 10),
  ('LIKED', '收到认可', '内容第一次收到他人的赞。', 'heart', '#ef4444', 40, 'LIKES_RECEIVED', 1),
  ('VALUABLE_POSTER', '高价值分享', '内容累计收到 5 个赞。', 'gem', '#8b5cf6', 45, 'LIKES_RECEIVED', 5),
  ('TRUSTED', '可信成员', '达到信任等级 1。', 'shield-check', '#6366f1', 50, 'TRUST_LEVEL', 1),
  ('SHARER', '资料分享者', '上传并分享了第一个文件。', 'paperclip', '#f97316', 60, 'UPLOADS', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), icon = VALUES(icon), color = VALUES(color), sort_order = VALUES(sort_order), rule_type = VALUES(rule_type), rule_threshold = VALUES(rule_threshold);

INSERT INTO roles (code, name, description)
VALUES
  ('USER', '普通用户', '社区普通成员默认角色'),
  ('MODERATOR', '版主', '具备协助内容治理和社区管理权限'),
  ('ADMIN', '管理员', '具备完整后台和站点管理权限')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO tag_groups (name, description, color, sort_order)
VALUES
  ('常用标签', '默认标签组，承载通用讨论标签。', '#64748b', 10),
  ('模型与推理', '围绕模型、推理框架和部署实践。', '#0ea5e9', 20),
  ('工程实践', '围绕应用开发、Agent、RAG 和工程落地。', '#22c55e', 30)
ON DUPLICATE KEY UPDATE description = VALUES(description), color = VALUES(color), sort_order = VALUES(sort_order);

UPDATE tags t
JOIN tag_groups tg ON tg.name = t.group_name
SET t.tag_group_id = tg.id
WHERE t.tag_group_id IS NULL;

UPDATE users u
SET topics_count = (SELECT COUNT(*) FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED'),
    posts_count = (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id AND p.status <> 'DELETED'),
    likes_received_count = (
      SELECT COUNT(*) FROM reactions r
      WHERE (r.target_type = 'TOPIC' AND r.target_id IN (SELECT t.id FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED'))
         OR (r.target_type = 'POST' AND r.target_id IN (SELECT p.id FROM posts p WHERE p.user_id = u.id AND p.status <> 'DELETED'))
    );

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = UPPER(u.role);

INSERT IGNORE INTO user_badges (user_id, badge_id)
SELECT u.id, b.id FROM users u JOIN badges b ON b.code = 'WELCOME' WHERE u.status <> 'DELETED';

INSERT IGNORE INTO user_badges (user_id, badge_id)
SELECT u.id, b.id FROM users u JOIN badges b ON b.code = 'FIRST_TOPIC'
WHERE EXISTS (SELECT 1 FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED');

INSERT IGNORE INTO user_badges (user_id, badge_id)
SELECT u.id, b.id FROM users u JOIN badges b ON b.code = 'FIRST_REPLY'
WHERE EXISTS (SELECT 1 FROM posts p WHERE p.user_id = u.id AND p.post_number > 1 AND p.status <> 'DELETED');

INSERT IGNORE INTO user_badges (user_id, badge_id)
SELECT u.id, b.id FROM users u JOIN badges b ON b.code = 'TOPIC_BUILDER'
WHERE (SELECT COUNT(*) FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED') >= 5;

INSERT IGNORE INTO user_badges (user_id, badge_id)
SELECT u.id, b.id FROM users u JOIN badges b ON b.code = 'ACTIVE_MEMBER'
WHERE (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id AND p.status <> 'DELETED') >= 10;

INSERT IGNORE INTO user_badges (user_id, badge_id)
SELECT u.id, b.id FROM users u JOIN badges b ON b.code = 'LIKED'
WHERE u.likes_received_count > 0;

INSERT IGNORE INTO user_badges (user_id, badge_id)
SELECT u.id, b.id FROM users u JOIN badges b ON b.code = 'VALUABLE_POSTER'
WHERE u.likes_received_count >= 5;

INSERT IGNORE INTO user_badges (user_id, badge_id)
SELECT u.id, b.id FROM users u JOIN badges b ON b.code = 'TRUSTED'
WHERE u.trust_level >= 1;

INSERT IGNORE INTO user_badges (user_id, badge_id)
SELECT u.id, b.id FROM users u JOIN badges b ON b.code = 'SHARER'
WHERE EXISTS (SELECT 1 FROM uploaded_files f WHERE f.user_id = u.id);

INSERT INTO chat_channels (id, name, slug, description, icon, position, min_trust_level, last_message_at)
VALUES
  (1, '大厅', 'lobby', '开放给所有成员的实时交流区，适合闲聊和快速协作。', 'messages-square', 1, 0, NOW()),
  (2, '部署实战', 'deployment', '围绕模型部署、推理服务和线上排障展开。', 'server-cog', 2, 1, NOW()),
  (3, '研究夜话', 'research', '偏研究讨论，适合分享论文、实验和评测想法。', 'flask-conical', 3, 2, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), icon = VALUES(icon), position = VALUES(position), min_trust_level = VALUES(min_trust_level), last_message_at = VALUES(last_message_at);

INSERT INTO chat_messages (id, channel_id, user_id, reply_to_message_id, raw_content, cooked_content, created_at)
VALUES
  (1, 1, 1, NULL, '欢迎来到聊天频道，适合处理不需要单独开帖的实时讨论。', '<p>欢迎来到聊天频道，适合处理不需要单独开帖的实时讨论。</p>', NOW()),
  (2, 1, 2, 1, '这个区挺适合同步排障进展，后面把结论再整理成帖子。', '<p>这个区挺适合同步排障进展，后面把结论再整理成帖子。</p>', NOW()),
  (3, 2, 1, NULL, '部署问题尽量附上模型大小、框架版本和 GPU 信息，方便别人复现。', '<p>部署问题尽量附上模型大小、框架版本和 GPU 信息，方便别人复现。</p>', NOW())
ON DUPLICATE KEY UPDATE raw_content = VALUES(raw_content), cooked_content = VALUES(cooked_content);

UPDATE chat_channels c
SET message_count = (SELECT COUNT(*) FROM chat_messages m WHERE m.channel_id = c.id AND m.status <> 'DELETED'),
    last_message_at = (SELECT MAX(m.created_at) FROM chat_messages m WHERE m.channel_id = c.id AND m.status <> 'DELETED');

INSERT INTO site_settings (setting_key, setting_value)
VALUES
  ('site_name', 'AI TEF'),
  ('notice_text', '真诚、友善、团结、专业，共建 AI 技术交流社区。'),
  ('community_guidelines_url', '/t/1/welcome-to-ai-tef'),
  ('allow_private_messages', 'true'),
  ('markdown_enabled', 'true')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
