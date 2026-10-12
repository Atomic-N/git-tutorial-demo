# 测试数据库配置指南

## 问题说明

测试数据初始化脚本中的字段名必须与实际数据库表结构完全一致。已修复以下字段名不匹配问题：

### 修正的字段名映射

| 表名 | 错误字段名 | 正确字段名 |
|------|-----------|-----------|
| users | topic_count, post_count, likes_received | topics_count, posts_count, likes_received_count |
| categories | minimum_trust_level | min_trust_level |
| tags | updated_at | (该字段不存在，已删除) |
| topics | view_count, reply_count, like_count, bookmark_count | views_count, replies_count, likes_count, bookmarks_count |
| topics | accepted_post_id, last_posted_at | solution_post_id, last_post_at |
| topics | ACTIVE | NORMAL |
| posts | like_count | likes_count |
| posts | ACTIVE | NORMAL |
| topic_tags | created_at | (该字段不存在，已删除) |

## 完整配置步骤

### 步骤1：创建测试数据库

打开MySQL命令行或客户端工具（如MySQL Workbench、Navicat等）：

```sql
-- 创建测试数据库
CREATE DATABASE IF NOT EXISTS forum_test 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

-- 验证数据库创建成功
SHOW DATABASES LIKE 'forum_test';
```

### 步骤2：创建数据库表结构

使用项目中的schema.sql创建表结构：

```bash
# Windows命令行
cd D:\other\AI\project\AI_TEF
mysql -u root -p forum_test < src\main\resources\db\schema.sql

# 或者使用PowerShell
Get-Content src\main\resources\db\schema.sql | mysql -u root -p forum_test
```

**注意**：schema.sql中第一行是 `CREATE DATABASE IF NOT EXISTS ai_tef`，它会创建生产数据库。测试数据库是独立的 `forum_test`。

### 步骤3：初始化测试数据

```bash
# Windows命令行
mysql -u root -p forum_test < test\resources\test-data-init.sql

# 或者使用PowerShell
Get-Content test\resources\test-data-init.sql | mysql -u root -p forum_test
```

### 步骤4：验证数据初始化

连接到测试数据库并验证数据：

```sql
USE forum_test;

-- 检查用户数据
SELECT id, username, role, status FROM users;
-- 应该显示4个用户：admin, moderator, testuser1, testuser2

-- 检查分类数据
SELECT id, name, slug FROM categories;
-- 应该显示5个分类

-- 检查话题数据
SELECT id, title, views_count, replies_count FROM topics;
-- 应该显示3个话题

-- 检查回复数据
SELECT id, topic_id, post_number FROM posts;
-- 应该显示5条回复

-- 检查总体数据量
SELECT 
  (SELECT COUNT(*) FROM users) AS user_count,
  (SELECT COUNT(*) FROM categories) AS category_count,
  (SELECT COUNT(*) FROM tags) AS tag_count,
  (SELECT COUNT(*) FROM topics) AS topic_count,
  (SELECT COUNT(*) FROM posts) AS post_count;
```

### 步骤5：配置测试连接

确认 `test/resources/test-db.properties` 中的密码已正确配置：

```properties
db.test.url=jdbc:mysql://localhost:3306/forum_test?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=UTC
db.test.username=root
db.test.password=你的MySQL密码
```

## 常见问题排查

### Q1: 提示"Unknown column"错误

**原因**：测试数据脚本中的字段名与实际表结构不匹配

**解决**：
1. 重新执行修复后的 `test-data-init.sql`
2. 或者先清空表后重新插入：
```sql
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE users;
TRUNCATE TABLE topics;
-- ... 其他表
SET FOREIGN_KEY_CHECKS = 1;
```

### Q2: 外键约束错误

**原因**：数据插入顺序不正确，或引用的ID不存在

**解决**：按照正确顺序执行：
1. 角色表 (roles)
2. 用户表 (users)
3. 用户角色关联 (user_roles)
4. 分类表 (categories)
5. 标签组 (tag_groups)
6. 标签 (tags)
7. 话题 (topics)
8. 回复 (posts)
9. 话题标签关联 (topic_tags)

### Q3: 字符编码问题

**原因**：数据库或表的字符集不是utf8mb4

**解决**：
```sql
-- 检查数据库字符集
SHOW CREATE DATABASE forum_test;

-- 如果不是utf8mb4，重新创建
DROP DATABASE forum_test;
CREATE DATABASE forum_test 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;
```

### Q4: 连接被拒绝

**原因**：MySQL服务未启动或密码错误

**解决**：
```bash
# Windows - 检查MySQL服务状态
net start | findstr MySQL

# 启动MySQL服务
net start MySQL80

# 测试连接
mysql -u root -p -e "SELECT 1"
```

## 测试账号信息

初始化完成后，可使用以下测试账号：

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | password123 | ADMIN | 系统管理员 |
| moderator | password123 | MODERATOR | 论坛版主 |
| testuser1 | password123 | USER | 普通用户1 |
| testuser2 | password123 | USER | 普通用户2 |

## 重置测试数据

如需重新初始化测试数据：

```bash
# 方法1：删除并重建数据库
mysql -u root -p -e "DROP DATABASE IF EXISTS forum_test"
mysql -u root -p -e "CREATE DATABASE forum_test DEFAULT CHARACTER SET utf8mb4"
mysql -u root -p forum_test < src\main\resources\db\schema.sql
mysql -u root -p forum_test < test\resources\test-data-init.sql

# 方法2：仅清空数据（保留表结构）
mysql -u root -p forum_test < test\resources\test-data-init.sql
# 脚本开头已包含TRUNCATE语句
```

## 下一步

配置完成后，可以运行测试：

```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=UserServiceTest
```

如果测试通过，说明数据库配置正确！
