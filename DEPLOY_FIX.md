# 私信功能修复 - 部署指南

## 问题原因
`DirectMessageDao.java` 的 `findThreadById` 方法SQL查询缺少 `unread_count` 字段，导致访问私信页面时抛出异常。

## 已修复的文件
- `src/main/java/com/aitef/forum/model/dao/DirectMessageDao.java`
  - 在 `findThreadById` 的SQL中添加了 `unread_count` 子查询
  - 调整了PreparedStatement参数绑定（从3个参数改为4个参数）

## 部署步骤

### 1. 找到您的Tomcat安装目录
常见位置：
- `D:\develop\tomcat\`
- `C:\Program Files\Apache Software Foundation\Tomcat X.X\`
- `C:\apache-tomcat-X.X\`

### 2. 停止Tomcat
在Tomcat的 `bin` 目录下运行：
```bash
shutdown.bat
```

或者通过任务管理器结束 `java.exe` 进程（如果进程名包含tomcat）

### 3. 清理旧的部署
删除以下文件和目录：
```
<TOMCAT_HOME>\webapps\ai-tef-forum.war
<TOMCAT_HOME>\webapps\ai-tef-forum\
```

### 4. 部署新的WAR文件
将以下文件复制到Tomcat的webapps目录：
```
从：D:\other\AI\project\AI_TEF\target\ai-tef-forum.war
到：<TOMCAT_HOME>\webapps\ai-tef-forum.war
```

### 5. 启动Tomcat
在Tomcat的 `bin` 目录下运行：
```bash
startup.bat
```

### 6. 验证部署
等待30秒后，访问：
```
http://localhost:8081/ai_tef_forum/messages
```

如果仍然报错，查看Tomcat日志：
```
<TOMCAT_HOME>\logs\catalina.out
<TOMCAT_HOME>\logs\localhost.YYYY-MM-DD.log
```

## 快速诊断脚本

创建一个批处理文件 `deploy.bat` 在项目根目录：

```batch
@echo off
echo ====================================
echo AI TEF 私信功能修复部署脚本
echo ====================================
echo.

REM 设置您的Tomcat路径（请修改为实际路径）
set TOMCAT_HOME=D:\develop\tomcat

if not exist "%TOMCAT_HOME%\bin\shutdown.bat" (
    echo [错误] 找不到Tomcat，请修改脚本中的 TOMCAT_HOME 变量
    echo 当前设置: %TOMCAT_HOME%
    pause
    exit /b 1
)

echo [1/5] 停止Tomcat...
call "%TOMCAT_HOME%\bin\shutdown.bat"
timeout /t 5 /nobreak

echo [2/5] 清理旧部署...
del "%TOMCAT_HOME%\webapps\ai-tef-forum.war" 2>nul
rmdir /s /q "%TOMCAT_HOME%\webapps\ai-tef-forum" 2>nul

echo [3/5] 编译项目...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo [错误] 编译失败
    pause
    exit /b 1
)

echo [4/5] 复制新WAR文件...
copy target\ai-tef-forum.war "%TOMCAT_HOME%\webapps\"

echo [5/5] 启动Tomcat...
call "%TOMCAT_HOME%\bin\startup.bat"

echo.
echo ====================================
echo 部署完成！
echo 等待30秒后访问：http://localhost:8081/ai_tef_forum/messages
echo ====================================
timeout /t 30 /nobreak

start http://localhost:8081/ai_tef_forum/messages
```

## 手动测试SQL（可选）

如果还是有问题，可以在MySQL中手动测试修复后的SQL：

```sql
SELECT t.*, partner.user_id AS partner_id, 
       u.username AS partner_username, 
       u.display_name AS partner_display_name, 
       u.avatar_url AS partner_avatar_url,
       last_msg.body_raw AS latest_preview, 
       last_msg.created_at AS latest_message_at,
       CASE
         WHEN self.last_read_at IS NULL THEN TRUE
         WHEN last_msg.created_at IS NULL THEN FALSE
         WHEN self.last_read_at < last_msg.created_at AND last_msg.sender_id <> 1 THEN TRUE
         ELSE FALSE
       END AS unread,
       (SELECT COUNT(*) FROM direct_messages dm
        WHERE dm.thread_id = t.id AND dm.status <> 'DELETED'
        AND dm.sender_id <> 1
        AND (self.last_read_at IS NULL OR dm.created_at > self.last_read_at)
       ) AS unread_count
FROM direct_message_participants self
JOIN direct_message_threads t ON t.id = self.thread_id
JOIN direct_message_participants partner ON partner.thread_id = t.id AND partner.user_id <> self.user_id
JOIN users u ON u.id = partner.user_id
LEFT JOIN direct_messages last_msg ON last_msg.id = (
    SELECT dm.id FROM direct_messages dm
    WHERE dm.thread_id = t.id AND dm.status <> 'DELETED'
    ORDER BY dm.created_at DESC
    LIMIT 1
)
WHERE self.thread_id = 3 AND self.user_id = 1
LIMIT 1;
```

这个查询应该成功返回结果，包含 `unread_count` 字段。

## 如果还是不行

1. 检查Tomcat日志中的完整错误堆栈
2. 确认WAR文件中包含了最新编译的类文件
3. 检查数据库连接是否正常
4. 验证编译的字节码版本与Tomcat的JDK版本匹配

需要进一步帮助，请提供：
- Tomcat的 `logs/catalina.out` 最后100行
- `logs/localhost.YYYY-MM-DD.log` 的错误信息
