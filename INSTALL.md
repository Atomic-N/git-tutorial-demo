# AI TEF 论坛 安装指南

本包提供两种部署方式：**方式 A（推荐）** 直接用编译好的 WAR，最快；**方式 B** 用 IDEA 跑源码，便于二次开发。

---

## 一、环境要求（两种方式都要先装）

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | **17**（必须，不能用更低） | 编译和运行 |
| MySQL | **8.0+**（含 9.x） | 数据库 |
| Tomcat | **10.1.x**（必须 10.x，**9.x 不兼容**） | 方式 A 需要 |
| Maven | 3.6+ | 方式 B 需要 |
| IDEA | **Ultimate 旗舰版** | 方式 B 需要（社区版无 Tomcat 集成，跑不了） |

> ⚠️ **最容易踩的坑**：Tomcat 必须是 10 系列。本项目用 Jakarta EE（`jakarta.servlet`），Tomcat 9 是旧的 `javax.servlet`，装错会报 `ClassNotFoundException`。

---

## 二、初始化数据库（两种方式都要做）

### 1. 导入数据（一条命令搞定）

包内 `database_complete.sql` 是完整的数据库（29 张表 + 全部演示数据，含管理员账号）。

**Windows（PowerShell）：**
```powershell
Get-Content -LiteralPath 'database_complete.sql' -Raw | & 'mysql.exe的完整路径' -uroot -p你的密码 --default-character-set=utf8mb4
```

**或命令行：**
```bash
mysql -uroot -p你的密码 < database_complete.sql
```

> 导入后会自动创建 `ai_tef` 库，包含 2 个演示用户、若干话题/帖子/私信等。

### 2. 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `admin123456` |
| 普通用户 | `demo` | `test123456` |

---

## 方式 A：WAR 部署（推荐，最快）

> 适用：只想运行项目、不改代码。**不需要 Maven、不需要 IDEA。**

### 第 1 步：把 WAR 丢进 Tomcat

把包内的 `ai-tef-forum.war` 复制到 Tomcat 的 `webapps/` 目录：

- Windows：`<Tomcat安装目录>\webapps\ai-tef-forum.war`
- 例：`D:\apache-tomcat-10.1.x\webapps\ai-tef-forum.war`

### 第 2 步：配置数据库密码（关键）

为安全，WAR 里的数据库密码是**空的**，必须用环境变量告诉应用你的 MySQL 密码。

设置环境变量 `AI_TEF_DB_PASSWORD`（必要时还有 `AI_TEF_DB_USER`、`AI_TEF_JDBC_URL`）：

**Windows 命令行启动 Tomcat（临时）：**
```cmd
set AI_TEF_DB_PASSWORD=你的MySQL密码
set AI_TEF_DB_USER=root
D:\apache-tomcat-10.1.x\bin\startup.bat
```

**永久生效：** 在系统环境变量里添加 `AI_TEF_DB_PASSWORD`，或编辑 Tomcat 的 `bin\setenv.bat`（没有就新建）写入：
```bat
set AI_TEF_DB_PASSWORD=你的MySQL密码
set AI_TEF_DB_USER=root
```

> 如果你的 MySQL 用户名不是 root、或库不在本机 3306，还需设置 `AI_TEF_DB_USER` 和 `AI_TEF_JDBC_URL`。

### 第 3 步：启动 Tomcat，访问

```
http://localhost:8080/ai-tef-forum/latest
```

看到论坛首页即成功。

---

## 方式 B：IDEA 源码运行（便于开发）

### 第 1 步：配置数据库密码

编辑 `src/main/resources/db.properties`，把密码改成你的：

```properties
jdbc.url=jdbc:mysql://localhost:3306/ai_tef?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
jdbc.username=root
jdbc.password=你的MySQL密码
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.pool.maximumPoolSize=10
jdbc.pool.minimumIdle=2
```

（也可不改文件，改用环境变量 `AI_TEF_DB_PASSWORD`，见方式 A 第 2 步）

### 第 2 步：IDEA 打开项目

`File → Open` 选择本项目根目录，等待 Maven 同步完成（会自动下载依赖）。

### 第 3 步：配置 Tomcat 运行项

1. 顶部 `Run → Edit Configurations`
2. 左上角 `+` → **Tomcat Server → Local**
3. **Server** 标签 → Application server 选你装的 **Tomcat 10.1**
4. **Deployment** 标签 → `+` → `Artifact` → 选 `ai-tef-forum:war exploded`
5. Application context 改成 `/ai-tef-forum`
6. 点 OK

### 第 4 步：点绿色 ▶ 启动，访问

```
http://localhost:8080/ai-tef-forum/latest
```

---

## 三、验证部署成功

访问 `http://localhost:8080/ai-tef-forum/latest`，应看到话题列表。
用 `admin / admin123456` 登录可进入管理功能。

---

## 四、常见问题

| 现象 | 原因 / 解决 |
|------|------------|
| 启动报 `ClassNotFoundException: jakarta.servlet...` | Tomcat 版本不对，必须是 **10.x**，不能用 9.x |
| 页面空白或 500，日志报连接失败 | `db.properties` 密码错，或没设 `AI_TEF_DB_PASSWORD` 环境变量 |
| 页面打开但没数据 | 没执行 `database_complete.sql` 导入，或导入到了别的库 |
| 端口 8080 被占用 | 改 Tomcat `conf/server.xml` 里的 `<Connector port="8080" .../>` |
| 中文乱码 | 确认库是 utf8mb4；MySQL 连接串含 `characterEncoding=utf8`（默认已含） |
| IDEA 社区版没有 Tomcat 选项 | 社区版不支持，需 Ultimate 旗舰版；或改用方式 A |
| `mvn package` 卡住 / 报错 | 检查 JDK 是否 17、网络能否下载依赖 |

---

## 五、目录说明

```
AI_TEF/
├── ai-tef-forum.war          # 编译好的应用（方式 A 用这个）
├── database_complete.sql     # 完整数据库脚本（建库+建表+演示数据）
├── src/                      # 源代码（方式 B 用）
│   └── main/resources/db.properties   # 数据库配置（改成你的密码）
├── pom.xml                   # Maven 依赖配置
└── INSTALL.md                # 本文件
```

---

## 六、数据库配置项参考

`db.properties` 支持的环境变量覆盖（优先级高于文件）：

| 配置项 | 环境变量 | 默认值 |
|--------|---------|--------|
| 连接 URL | `AI_TEF_JDBC_URL` | `jdbc:mysql://localhost:3306/ai_tef...` |
| 用户名 | `AI_TEF_DB_USER` | `root` |
| 密码 | `AI_TEF_DB_PASSWORD` | （空） |
| 驱动类 | `AI_TEF_JDBC_DRIVER` | `com.mysql.cj.jdbc.Driver` |
| 连接池上限 | `AI_TEF_DB_POOL_MAX` | 10 |
| 连接池下限 | `AI_TEF_DB_POOL_MIN` | 2 |
