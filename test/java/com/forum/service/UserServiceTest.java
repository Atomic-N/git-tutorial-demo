package com.forum.service;

import com.forum.dao.UserDao;
import com.forum.model.User;
import com.forum.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务测试类
 * 测试用户注册、登录等核心功能
 */
@DisplayName("用户服务测试")
public class UserServiceTest {

    private UserService userService;

    @Mock
    private UserDao userDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userDao);
    }

    @Test
    @DisplayName("测试用例1：用户名为空提交")
    void testRegisterWithEmptyUsername() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.register("", "test@example.com", "password123", "Test User");
        });
        assertEquals("用户名只能包含字母、数字和下划线，长度 3-40 位", exception.getMessage());
    }

    @Test
    @DisplayName("测试用例2：密码长度小于8位提交")
    void testRegisterWithShortPassword() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.register("testuser", "test@example.com", "pass", "Test User");
        });
        assertEquals("密码至少需要 8 位", exception.getMessage());
    }

    @Test
    @DisplayName("测试用例3：邮箱格式不符合规范提交")
    void testRegisterWithInvalidEmail() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.register("testuser", "invalid-email", "password123", "Test User");
        });
        assertEquals("请输入有效邮箱", exception.getMessage());
    }

    @Test
    @DisplayName("测试用例4：使用已注册的用户名提交")
    void testRegisterWithExistingUsername() {
        when(userDao.existsByUsernameOrEmail(anyString(), anyString())).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.register("existinguser", "new@example.com", "password123", "Test User");
        });
        assertEquals("用户名或邮箱已被使用", exception.getMessage());
    }

    @Test
    @DisplayName("测试用例5：全部信息输入正确且符合规范提交")
    void testRegisterSuccess() {
        when(userDao.existsByUsernameOrEmail(anyString(), anyString())).thenReturn(false);
        when(userDao.create(any(User.class))).thenReturn(1L);

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");
        when(userDao.findById(1L)).thenReturn(Optional.of(mockUser));

        User result = userService.register("testuser", "test@example.com", "password123", "Test User");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userDao, times(1)).create(any(User.class));
    }

    @Test
    @DisplayName("测试用例6：输入正确用户名、输入错误密码提交")
    void testLoginWithWrongPassword() {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setPasswordHash("$2a$10$hashedpassword");
        mockUser.setStatus("ACTIVE");

        when(userDao.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(mockUser));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.login("testuser", "wrongpassword");
        });
        assertEquals("账号或密码不正确", exception.getMessage());
    }

    @Test
    @DisplayName("测试用例7：输入不存在的用户名、输入任意密码提交")
    void testLoginWithNonExistentUser() {
        when(userDao.findByUsernameOrEmail(anyString())).thenReturn(Optional.empty());

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.login("nonexistent", "password123");
        });
        assertEquals("账号或密码不正确", exception.getMessage());
    }

    @Test
    @DisplayName("测试用例8：输入已禁用的账号提交")
    void testLoginWithDisabledAccount() {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setPasswordHash("$2a$10$hashedpassword");
        mockUser.setStatus("SUSPENDED");

        when(userDao.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(mockUser));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.login("testuser", "password123");
        });
        assertEquals("账号当前不可用", exception.getMessage());
    }

    @Test
    @DisplayName("测试用例9：用户名格式验证 - 包含非法字符")
    void testRegisterWithInvalidCharacters() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.register("test@user", "test@example.com", "password123", "Test User");
        });
        assertEquals("用户名只能包含字母、数字和下划线，长度 3-40 位", exception.getMessage());
    }

    @Test
    @DisplayName("测试用例10：用户名长度边界测试 - 少于3位")
    void testRegisterWithUsernameTooShort() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.register("ab", "test@example.com", "password123", "Test User");
        });
        assertEquals("用户名只能包含字母、数字和下划线，长度 3-40 位", exception.getMessage());
    }

    @Test
    @DisplayName("测试用例11：用户名长度边界测试 - 正好3位")
    void testRegisterWithUsernameExactly3Chars() {
        when(userDao.existsByUsernameOrEmail(anyString(), anyString())).thenReturn(false);
        when(userDao.create(any(User.class))).thenReturn(1L);

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("abc");
        when(userDao.findById(1L)).thenReturn(Optional.of(mockUser));

        User result = userService.register("abc", "test@example.com", "password123", "Test User");

        assertNotNull(result);
        assertEquals("abc", result.getUsername());
    }

    @Test
    @DisplayName("测试用例12：邮箱长度超过120位")
    void testRegisterWithEmailTooLong() {
        String longEmail = "a".repeat(110) + "@example.com"; // 超过120位

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.register("testuser", longEmail, "password123", "Test User");
        });
        assertEquals("请输入有效邮箱", exception.getMessage());
    }
}
