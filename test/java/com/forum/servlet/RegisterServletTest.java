package com.forum.servlet;

import com.forum.model.User;
import com.forum.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 注册Servlet测试
 * 测试用户注册请求处理
 */
@DisplayName("注册Servlet测试")
public class RegisterServletTest {

    private RegisterServlet registerServlet;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        registerServlet = new RegisterServlet();
        // 注入mock的service
        registerServlet.setUserService(userService);
    }

    @Test
    @DisplayName("测试GET请求 - 显示注册页面")
    void testDoGet() throws Exception {
        when(request.getRequestDispatcher("/WEB-INF/views/register.jsp")).thenReturn(dispatcher);

        registerServlet.doGet(request, response);

        verify(dispatcher, times(1)).forward(request, response);
    }

    @Test
    @DisplayName("测试POST请求 - 注册成功")
    void testDoPost_Success() throws Exception {
        when(request.getParameter("username")).thenReturn("newuser");
        when(request.getParameter("email")).thenReturn("newuser@example.com");
        when(request.getParameter("password")).thenReturn("password123");
        when(request.getParameter("confirmPassword")).thenReturn("password123");
        when(request.getParameter("displayName")).thenReturn("New User");
        when(request.getContextPath()).thenReturn("/forum");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("newuser");
        when(userService.register(anyString(), anyString(), anyString(), anyString())).thenReturn(mockUser);

        registerServlet.doPost(request, response);

        verify(response, times(1)).sendRedirect("/forum/login?registered=true");
    }

    @Test
    @DisplayName("测试POST请求 - 密码不匹配")
    void testDoPost_PasswordMismatch() throws Exception {
        when(request.getParameter("username")).thenReturn("newuser");
        when(request.getParameter("email")).thenReturn("newuser@example.com");
        when(request.getParameter("password")).thenReturn("password123");
        when(request.getParameter("confirmPassword")).thenReturn("password456");
        when(request.getRequestDispatcher("/WEB-INF/views/register.jsp")).thenReturn(dispatcher);

        registerServlet.doPost(request, response);

        verify(request, times(1)).setAttribute(eq("error"), contains("密码"));
        verify(dispatcher, times(1)).forward(request, response);
    }

    @Test
    @DisplayName("测试POST请求 - 用户名已存在")
    void testDoPost_UsernameExists() throws Exception {
        when(request.getParameter("username")).thenReturn("existinguser");
        when(request.getParameter("email")).thenReturn("new@example.com");
        when(request.getParameter("password")).thenReturn("password123");
        when(request.getParameter("confirmPassword")).thenReturn("password123");
        when(request.getRequestDispatcher("/WEB-INF/views/register.jsp")).thenReturn(dispatcher);

        when(userService.register(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("用户名已被使用"));

        registerServlet.doPost(request, response);

        verify(request, times(1)).setAttribute(eq("error"), anyString());
        verify(dispatcher, times(1)).forward(request, response);
    }
}
