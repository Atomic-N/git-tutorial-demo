USE ai_tef;

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
