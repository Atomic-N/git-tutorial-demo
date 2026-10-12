# AI TEF - AI 技术交流论坛

基于 `JDK 17 + Tomcat 10 + MySQL + Servlet + JSP + JSTL/EL + JDBC + Axios + Maven` 的 MVC 分层论坛项目。界面参考 Linux.do / Discourse 的信息架构和交互风格，实现第一阶段 MVP。

## 已实现功能

- 用户注册、登录、退出
- 首页/最新/新话题/未读/排行榜/书签话题列表
- 分类列表、分类详情
- 标签分组、标签详情
- 话题详情、浏览计数、阅读进度
- 登录用户创建话题、发表回复
- 话题点赞、回复点赞、话题收藏，使用 Axios Ajax 无刷新切换
- 搜索：关键词、分类、标签、作者筛选
- 用户主页、个人资料维护
- 通知列表、全部已读
- MySQL 建表脚本和种子数据

## 项目结构

```text
src/main/java/com/aitef/forum
  controller   Servlet 控制层
  filter       编码和登录过滤器
  model
    entity     数据实体
    dto        页面/接口传输对象
    dao        JDBC 数据访问
    service    业务逻辑
  util         数据库、密码、HTML、分页等工具

src/main/webapp
  WEB-INF/views JSP 视图层
  static        CSS、JS、图片

src/main/resources/db
  schema.sql    建表脚本
  seed.sql      初始数据
```

## 运行方式

详细的安装部署步骤见 **[INSTALL.md](INSTALL.md)**。

支持两种方式：

- **方式 A（推荐）**：直接用编译好的 `ai-tef-forum.war` 部署到 Tomcat 10。
- **方式 B**：用 IDEA（旗舰版）打开源码运行，便于二次开发。

数据库用包内 `database_complete.sql` 一条命令导入即可（含建库、建表、演示数据）。

访问地址：[http://localhost:8080/ai-tef-forum/latest](http://localhost:8080/ai-tef-forum/latest)

## 默认账号

- 管理员：`admin / admin123456`
- 普通用户：`demo / test123456`

## 数据库配置

默认配置在 `src/main/resources/db.properties`（**密码请改成你自己的**）：

```properties
jdbc.url=jdbc:mysql://localhost:3306/ai_tef?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
jdbc.username=root
jdbc.password=你的MySQL密码
```

也可以用环境变量覆盖（优先级高于配置文件，WAR 部署推荐用这种方式，无需改文件）：

- `AI_TEF_JDBC_URL`
- `AI_TEF_DB_USER`
- `AI_TEF_DB_PASSWORD`
- `AI_TEF_DB_POOL_MAX`
- `AI_TEF_DB_POOL_MIN`
