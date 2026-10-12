USE ai_tef;

INSERT INTO users (id, username, email, password_hash, display_name, avatar_url, bio, location, trust_level, role)
VALUES
  (1, 'admin', 'admin@aitef.local', 'pbkdf2$120000$lqVtPdojREp0XE1fw61UNw==$ajVp+TQNpY7CgGBUv+zx/Cd3BG9kG1H8CLgQnIcw5rM=', '站务管理员', 'https://api.dicebear.com/9.x/initials/svg?seed=admin', '负责维护 AI TEF 社区秩序与基础数据。', '长沙', 4, 'ADMIN'),
  (2, 'demo', 'demo@aitef.local', 'pbkdf2$120000$AyXbkQX2VLEtiiEtQBgKOw==$PxHbTIb3ccy/AaauXtWvqfrMgV3cyHe68EMc1z53H+Q=', '演示用户', 'https://api.dicebear.com/9.x/initials/svg?seed=demo', '喜欢折腾 Agent、RAG 和模型部署。', '广州', 1, 'USER')
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT INTO categories (id, parent_id, name, slug, description, color, text_color, icon, position, min_trust_level)
VALUES
  (1, NULL, '开发调优', 'develop', '围绕 AI 应用开发、调试、部署、优化、安全和工程实践展开讨论。', '#32c3c3', '#082f49', 'code-2', 1, 0),
  (2, NULL, '模型研究', 'models', '大模型、开源模型、多模态、推理框架、评测与论文解读。', '#8b5cf6', '#ffffff', 'brain', 2, 0),
  (3, NULL, '资源荟萃', 'resources', '工具、数据集、教程、开源项目和可复用资料分享。', '#12a89d', '#ffffff', 'share-2', 3, 0),
  (4, NULL, '快问快答', 'qa', '适合短问题、排错求助和快速经验交流。', '#16b176', '#ffffff', 'circle-help', 4, 0),
  (5, NULL, '文档共建', 'wiki', '沉淀 AI 技术笔记、最佳实践、课程资料和社区共建文档。', '#9cb6c4', '#111827', 'book-open', 5, 0),
  (6, NULL, '站务公告', 'announcements', '社区规则、功能更新、活动通知和管理公告。', '#0ea5e9', '#ffffff', 'megaphone', 6, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO tags (id, name, slug, group_name, color, icon)
VALUES
  (1, '人工智能', 'ai', '特殊标签', '#bd93f9', 'brain'),
  (2, 'ChatGPT', 'chatgpt', '模型生态', '#64748b', 'bot'),
  (3, 'Claude', 'claude', '模型生态', '#64748b', 'sparkles'),
  (4, 'DeepSeek', 'deepseek', '模型生态', '#64748b', 'bot'),
  (5, 'RAG', 'rag', '开发实践', '#669d34', 'database-zap'),
  (6, 'Agent', 'agent', '开发实践', '#669d34', 'workflow'),
  (7, '软件开发', 'software-dev', '开发实践', '#669d34', 'file-code-2'),
  (8, '开源项目', 'open-source', '推广标签', '#22c55e', 'package-open'),
  (9, '教程', 'tutorial', '资源标签', '#0ea5e9', 'graduation-cap'),
  (10, '公告', 'announcement', '社区官方', '#0ea5e9', 'megaphone')
ON DUPLICATE KEY UPDATE name = VALUES(name), group_name = VALUES(group_name);

INSERT INTO topics (id, user_id, category_id, title, slug, excerpt, views_count, replies_count, likes_count, pinned, last_post_at, created_at)
VALUES
  (1, 1, 6, '欢迎来到 AI TEF：新的 AI 技术交流社区', 'welcome-to-ai-tef', '这里是社区的第一篇公告，欢迎分享模型使用、开发调优和工程实践。', 286, 1, 5, TRUE, NOW(), NOW()),
  (2, 2, 1, 'RAG 项目从原型到上线，大家一般怎么拆模块？', 'rag-project-module-design', '最近在做知识库问答，想听听检索、重排、提示词和权限控制的工程拆分经验。', 173, 1, 3, FALSE, NOW(), NOW()),
  (3, 2, 4, '本地部署模型时显存不够，优先从哪里优化？', 'local-model-vram-optimization', '尝试量化、上下文长度、batch size 等参数后仍然紧张，求一套排查思路。', 91, 0, 1, FALSE, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), excerpt = VALUES(excerpt);

INSERT INTO posts (id, topic_id, user_id, post_number, raw_content, cooked_content, likes_count, created_at)
VALUES
  (1, 1, 1, 1, '欢迎来到 AI TEF。这里鼓励真诚、友善、团结、专业的 AI 技术交流。', '<p>欢迎来到 AI TEF。这里鼓励真诚、友善、团结、专业的 AI 技术交流。</p>', 5, NOW()),
  (2, 1, 2, 2, '已经准备好把最近做 Agent 的经验整理出来了。', '<p>已经准备好把最近做 Agent 的经验整理出来了。</p>', 2, NOW()),
  (3, 2, 2, 1, '我的初步想法是把数据接入、分块、向量化、检索、重排和回答生成拆开，但不确定权限过滤放哪一层更合适。', '<p>我的初步想法是把数据接入、分块、向量化、检索、重排和回答生成拆开，但不确定权限过滤放哪一层更合适。</p>', 3, NOW()),
  (4, 2, 1, 2, '建议权限过滤尽量前置到检索候选生成阶段，同时在回答前再做一次引用来源校验。', '<p>建议权限过滤尽量前置到检索候选生成阶段，同时在回答前再做一次引用来源校验。</p>', 4, NOW()),
  (5, 3, 2, 1, '本地部署 7B 模型时显存占用比预期高很多，大家一般先看哪些指标？', '<p>本地部署 7B 模型时显存占用比预期高很多，大家一般先看哪些指标？</p>', 1, NOW())
ON DUPLICATE KEY UPDATE raw_content = VALUES(raw_content);

UPDATE topics SET last_post_id = 2, replies_count = 1 WHERE id = 1;
UPDATE topics SET last_post_id = 4, replies_count = 1 WHERE id = 2;
UPDATE topics SET last_post_id = 5, replies_count = 0 WHERE id = 3;

INSERT IGNORE INTO topic_tags (topic_id, tag_id)
VALUES
  (1, 10), (1, 1),
  (2, 5), (2, 6), (2, 7),
  (3, 1), (3, 9);

UPDATE categories c
SET topic_count = (SELECT COUNT(*) FROM topics t WHERE t.category_id = c.id AND t.status <> 'DELETED'),
    post_count = (
      SELECT COUNT(*)
      FROM posts p
      JOIN topics t ON t.id = p.topic_id
      WHERE t.category_id = c.id AND p.status <> 'DELETED'
    );

UPDATE tags tg
SET topic_count = (SELECT COUNT(*) FROM topic_tags tt WHERE tt.tag_id = tg.id);

INSERT INTO chat_channels (id, name, slug, description, icon, position, min_trust_level, last_message_at)
VALUES
  (1, '大厅', 'lobby', '开放给所有成员的实时交流区，适合闲聊和快速协作。', 'messages-square', 1, 0, NOW()),
  (2, '部署实战', 'deployment', '围绕模型部署、推理服务和线上排障展开。', 'server-cog', 2, 1, NOW()),
  (3, '研究夜话', 'research', '偏研究讨论，适合分享论文、实验和评测想法。', 'flask-conical', 3, 2, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO chat_messages (id, channel_id, user_id, reply_to_message_id, raw_content, cooked_content, created_at)
VALUES
  (1, 1, 1, NULL, '欢迎来到聊天频道，适合处理不需要单独开帖的实时讨论。', '<p>欢迎来到聊天频道，适合处理不需要单独开帖的实时讨论。</p>', NOW()),
  (2, 1, 2, 1, '这个区挺适合同步排障进展，后面把结论再整理成帖子。', '<p>这个区挺适合同步排障进展，后面把结论再整理成帖子。</p>', NOW()),
  (3, 2, 1, NULL, '部署问题尽量附上模型大小、框架版本和 GPU 信息，方便别人复现。', '<p>部署问题尽量附上模型大小、框架版本和 GPU 信息，方便别人复现。</p>', NOW())
ON DUPLICATE KEY UPDATE raw_content = VALUES(raw_content), cooked_content = VALUES(cooked_content);

UPDATE chat_channels c
SET message_count = (SELECT COUNT(*) FROM chat_messages m WHERE m.channel_id = c.id AND m.status <> 'DELETED'),
    last_message_at = (SELECT MAX(m.created_at) FROM chat_messages m WHERE m.channel_id = c.id AND m.status <> 'DELETED');
