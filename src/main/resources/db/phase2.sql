USE ai_tef;
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

UPDATE users u
SET topics_count = (SELECT COUNT(*) FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED'),
    posts_count = (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id AND p.status <> 'DELETED'),
    likes_received_count = (
      SELECT COUNT(*) FROM reactions r
      WHERE (r.target_type = 'TOPIC' AND r.target_id IN (SELECT t.id FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED'))
         OR (r.target_type = 'POST' AND r.target_id IN (SELECT p.id FROM posts p WHERE p.user_id = u.id AND p.status <> 'DELETED'))
    );

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
