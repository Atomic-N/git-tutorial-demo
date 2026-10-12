package com.forum.dao;

import com.forum.model.User;
import com.forum.util.DatabaseUtil;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserDao数据访问层测试
 * 测试用户数据库操作
 */
@DisplayName("UserDao测试")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserDaoTest {

    private UserDao userDao;
    private Connection connection;

    @BeforeAll
    void setUpDatabase() throws Exception {
        // 初始化测试数据库连接
        connection = DatabaseUtil.getConnection();
        userDao = new UserDao();
    }

    @BeforeEach
    void setUp() throws Exception {
        // 清理测试数据
        connection.createStatement().execute("DELETE FROM users WHERE username LIKE 'test_%'");
    }

    @AfterAll
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("测试创建用户")
    void testCreateUser() throws Exception {
        User user = new User();
        user.setUsername("test_user_001");
        user.setEmail("test001@example.com");
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setDisplayName("Test User 001");
        user.setTrustLevel(0);
        user.setRole("USER");
        user.setStatus("ACTIVE");

        Long userId = userDao.create(connection, user);

        assertNotNull(userId);
        assertTrue(userId > 0);
    }

    @Test
    @DisplayName("测试根据ID查找用户")
    void testFindById() throws Exception {
        // 先创建用户
        User user = createTestUser("test_user_002", "test002@example.com");
        Long userId = userDao.create(connection, user);

        // 查找用户
        Optional<User> found = userDao.findById(userId);

        assertTrue(found.isPresent());
        assertEquals("test_user_002", found.get().getUsername());
        assertEquals("test002@example.com", found.get().getEmail());
    }

    @Test
    @DisplayName("测试根据用户名查找用户")
    void testFindByUsername() throws Exception {
        User user = createTestUser("test_user_003", "test003@example.com");
        userDao.create(connection, user);

        Optional<User> found = userDao.findByUsernameOrEmail("test_user_003");

        assertTrue(found.isPresent());
        assertEquals("test_user_003", found.get().getUsername());
    }

    @Test
    @DisplayName("测试根据邮箱查找用户")
    void testFindByEmail() throws Exception {
        User user = createTestUser("test_user_004", "test004@example.com");
        userDao.create(connection, user);

        Optional<User> found = userDao.findByUsernameOrEmail("test004@example.com");

        assertTrue(found.isPresent());
        assertEquals("test004@example.com", found.get().getEmail());
    }

    @Test
    @DisplayName("测试用户名或邮箱是否存在")
    void testExistsByUsernameOrEmail() throws Exception {
        User user = createTestUser("test_user_005", "test005@example.com");
        userDao.create(connection, user);

        assertTrue(userDao.existsByUsernameOrEmail("test_user_005", "other@example.com"));
        assertTrue(userDao.existsByUsernameOrEmail("other_user", "test005@example.com"));
        assertFalse(userDao.existsByUsernameOrEmail("nonexistent", "nonexistent@example.com"));
    }

    @Test
    @DisplayName("测试更新用户最后在线时间")
    void testUpdateLastSeen() throws Exception {
        User user = createTestUser("test_user_006", "test006@example.com");
        Long userId = userDao.create(connection, user);

        userDao.updateLastSeen(userId);

        Optional<User> updated = userDao.findById(userId);
        assertTrue(updated.isPresent());
        assertNotNull(updated.get().getLastSeenAt());
    }

    @Test
    @DisplayName("测试更新用户状态")
    void testUpdateStatus() throws Exception {
        User user = createTestUser("test_user_007", "test007@example.com");
        Long userId = userDao.create(connection, user);

        userDao.updateStatus(connection, userId, "SUSPENDED");

        Optional<User> updated = userDao.findById(userId);
        assertTrue(updated.isPresent());
        assertEquals("SUSPENDED", updated.get().getStatus());
    }

    @Test
    @DisplayName("测试更新用户信任等级")
    void testUpdateTrustLevel() throws Exception {
        User user = createTestUser("test_user_008", "test008@example.com");
        Long userId = userDao.create(connection, user);

        userDao.updateTrustLevel(connection, userId, 3);

        Optional<User> updated = userDao.findById(userId);
        assertTrue(updated.isPresent());
        assertEquals(3, updated.get().getTrustLevel());
    }

    @Test
    @DisplayName("测试增加用户话题数")
    void testIncrementTopicCount() throws Exception {
        User user = createTestUser("test_user_009", "test009@example.com");
        Long userId = userDao.create(connection, user);

        userDao.incrementTopicCount(connection, userId);

        Optional<User> updated = userDao.findById(userId);
        assertTrue(updated.isPresent());
        assertEquals(1, updated.get().getTopicCount());
    }

    @Test
    @DisplayName("测试增加用户回复数")
    void testIncrementPostCount() throws Exception {
        User user = createTestUser("test_user_010", "test010@example.com");
        Long userId = userDao.create(connection, user);

        userDao.incrementPostCount(connection, userId);

        Optional<User> updated = userDao.findById(userId);
        assertTrue(updated.isPresent());
        assertEquals(1, updated.get().getPostCount());
    }

    @Test
    @DisplayName("测试查询用户总数")
    void testCount() throws Exception {
        long countBefore = userDao.count();

        User user1 = createTestUser("test_user_011", "test011@example.com");
        User user2 = createTestUser("test_user_012", "test012@example.com");
        userDao.create(connection, user1);
        userDao.create(connection, user2);

        long countAfter = userDao.count();

        assertEquals(countBefore + 2, countAfter);
    }

    @Test
    @DisplayName("测试查询近期用户")
    void testFindRecent() throws Exception {
        User user1 = createTestUser("test_user_013", "test013@example.com");
        User user2 = createTestUser("test_user_014", "test014@example.com");
        userDao.create(connection, user1);
        userDao.create(connection, user2);

        var recentUsers = userDao.findRecent(10);

        assertNotNull(recentUsers);
        assertTrue(recentUsers.size() >= 2);
    }

    // 辅助方法
    private User createTestUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setDisplayName(username);
        user.setTrustLevel(0);
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setTopicCount(0);
        user.setPostCount(0);
        user.setLikesReceived(0);
        return user;
    }
}
