# AI技术交流综合论坛 - 测试文档

## 测试概述

本项目为AI技术交流综合论坛系统编写了完整的测试代码，覆盖Service层、DAO层和Servlet层的核心功能。

## 测试环境

### 硬件环境
- CPU：英特尔酷睿四核处理器
- 内存：4GB及以上
- 存储：500GB机械硬盘

### 软件环境
- 操作系统：Windows 10/11 或 Linux
- JDK：17
- 数据库：MySQL 8.0
- 构建工具：Maven 3.8+
- 测试框架：JUnit 5.9.3 + Mockito 5.3.1

## 目录结构

```
test/
├── java/com/forum/
│   ├── service/              # Service层测试
│   │   ├── UserServiceTest.java
│   │   ├── TopicServiceTest.java
│   │   ├── InteractionServiceTest.java
│   │   └── AdminServiceTest.java
│   ├── dao/                  # DAO层测试
│   │   ├── UserDaoTest.java
│   │   └── TopicDaoTest.java
│   └── servlet/              # Servlet层测试
│       ├── RegisterServletTest.java
│       ├── LoginServletTest.java
│       └── TopicServletTest.java
└── resources/
    ├── test-db.properties    # 测试数据库配置
    └── test-data-init.sql    # 测试数据初始化脚本
```

## 测试覆盖范围

### 1. 用户模块测试（UserServiceTest）
- ✅ 测试用例1：用户名为空提交
- ✅ 测试用例2：密码长度小于8位提交
- ✅ 测试用例3：邮箱格式不符合规范提交
- ✅ 测试用例4：使用已注册的用户名提交
- ✅ 测试用例5：全部信息输入正确且符合规范提交
- ✅ 测试用例6：输入正确用户名、输入错误密码提交
- ✅ 测试用例7：输入不存在的用户名、输入任意密码提交
- ✅ 测试用例8：输入已禁用的账号提交
- ✅ 测试用例9：用户名格式验证 - 包含非法字符
- ✅ 测试用例10：用户名长度边界测试
- ✅ 测试用例11：邮箱长度边界测试

### 2. 话题模块测试（TopicServiceTest）
- ✅ 测试用例1：未登录状态发布话题
- ✅ 测试用例2：发布话题时标题为空
- ✅ 测试用例3：发布话题时内容为空
- ✅ 测试用例4：标题长度超过200位
- ✅ 测试用例5：填写完整标题、分类、正文后提交话题
- ✅ 测试用例6：在话题详情页输入回复内容提交
- ✅ 测试用例7：引用某一楼层回复并提交
- ✅ 测试用例8：搜索话题 - 按关键词
- ✅ 测试用例9：搜索话题 - 按分类
- ✅ 测试用例10：搜索话题 - 按标签
- ✅ 测试用例11：搜索话题 - 仅看已解决

### 3. 互动模块测试（InteractionServiceTest）
- ✅ 测试用例1：点击话题点赞按钮
- ✅ 测试用例2：再次点击已点赞的话题
- ✅ 测试用例3：点击话题收藏按钮
- ✅ 测试用例4：在个人书签页取消收藏
- ✅ 测试用例5：对回复内容点赞
- ✅ 测试用例6：对回复取消点赞
- ✅ 测试用例7：点赞不存在的话题
- ✅ 测试用例8：收藏不存在的话题
- ✅ 测试用例9：并发点赞测试

### 4. 后台管理测试（AdminServiceTest）
- ✅ 测试用例1：普通用户访问后台地址
- ✅ 测试用例2：版主访问后台
- ✅ 测试用例3：管理员访问后台
- ✅ 测试用例4：管理员删除违规话题
- ✅ 测试用例5：为用户分配版主角色
- ✅ 测试用例6：处理用户举报
- ✅ 测试用例7：修改站点配置
- ✅ 测试用例8-15：用户管理、内容治理相关测试

### 5. DAO层测试
- ✅ UserDaoTest：用户数据访问测试（12个测试用例）
- ✅ TopicDaoTest：话题数据访问测试（14个测试用例）

### 6. Servlet层测试
- ✅ RegisterServletTest：注册请求处理测试
- ✅ LoginServletTest：登录请求处理测试
- ✅ TopicServletTest：话题操作请求处理测试

## 使用说明

### 1. 配置测试数据库

```bash
# 创建测试数据库
mysql -u root -p
CREATE DATABASE forum_test DEFAULT CHARACTER SET utf8mb4;

# 执行表结构创建脚本（使用生产环境的DDL脚本）
mysql -u root -p forum_test < database/schema.sql

# 初始化测试数据
mysql -u root -p forum_test < test/resources/test-data-init.sql
```

### 2. 配置数据库连接

编辑 `test/resources/test-db.properties`：

```properties
db.test.url=jdbc:mysql://localhost:3306/forum_test?useUnicode=true&characterEncoding=utf8mb4
db.test.username=root
db.test.password=your_password
```

### 3. 运行测试

#### 运行所有测试
```bash
mvn test
```

#### 运行指定测试类
```bash
mvn test -Dtest=UserServiceTest
mvn test -Dtest=TopicServiceTest
```

#### 运行指定测试方法
```bash
mvn test -Dtest=UserServiceTest#testRegisterSuccess
```

#### 生成测试报告
```bash
mvn test
# 测试报告位于：target/surefire-reports/
```

#### 生成代码覆盖率报告
```bash
mvn test
mvn jacoco:report
# 覆盖率报告位于：target/site/jacoco/index.html
```

## 测试注意事项

### 1. 数据隔离
- 所有测试使用独立的测试数据库 `forum_test`
- 每个测试方法执行前会清理相关测试数据
- 测试数据与生产数据完全隔离

### 2. 测试顺序
- 单元测试可以并行执行，互不影响
- 集成测试建议按顺序执行，避免数据冲突

### 3. Mock对象使用
- Service层测试：Mock DAO层对象
- Servlet层测试：Mock Request、Response、Session对象
- DAO层测试：使用真实数据库连接（集成测试）

### 4. 密码加密
测试数据中的默认密码均为 `password123`，已使用BCrypt加密

### 5. 性能测试
当前测试主要关注功能正确性，性能测试需要单独配置JMeter或类似工具

## 测试结果示例

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.forum.service.UserServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.forum.service.TopicServiceTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.forum.service.InteractionServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.forum.service.AdminServiceTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.forum.dao.UserDaoTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.forum.dao.TopicDaoTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 73, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

## 扩展测试建议

### 1. 集成测试扩展
- 添加端到端测试，使用Selenium模拟用户操作
- 添加API集成测试，测试完整请求响应流程

### 2. 性能测试
- 使用JMeter进行并发压力测试
- 测试数据库连接池在高并发下的表现
- 测试缓存机制的有效性

### 3. 安全测试
- SQL注入测试
- XSS跨站脚本测试
- CSRF跨站请求伪造测试
- 密码强度测试

### 4. 兼容性测试
- 多浏览器兼容性测试（Chrome、Firefox、Safari、Edge）
- 移动端响应式测试
- 不同MySQL版本兼容性测试

## 常见问题

### Q1: 测试运行失败，提示数据库连接错误
**A:** 检查以下几点：
1. 确保MySQL服务已启动
2. 验证test-db.properties中的数据库连接配置
3. 确认forum_test数据库已创建
4. 检查数据库用户权限

### Q2: Mock对象注入失败
**A:** 确保在测试类中正确使用了Mockito注解：
```java
@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
}
```

### Q3: 测试数据残留影响其他测试
**A:** 在@BeforeEach或@AfterEach中清理测试数据：
```java
@BeforeEach
void setUp() throws Exception {
    connection.createStatement().execute("DELETE FROM users WHERE username LIKE 'test_%'");
}
```

## 联系方式

如有测试相关问题，请联系开发团队：
- 项目负责人：于泽涛 (202401150631)
- 测试负责人：欧阳欣玥 (202401150602)

---
**最后更新：2026年6月23日**
