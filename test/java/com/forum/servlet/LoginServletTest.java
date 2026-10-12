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
 * 登录Servlet测试
 * 测试用户登录请求处理
 */
@DisplayName("登录Servlet测试")
public class LoginServletTest {

    private LoginServlet loginServlet;

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
        loginServlet = new LoginServlet();
        loginServlet.setUserService(userService);
    }

    @Test
    @DisplayName("测试GET请求 - 显示登录页面")
    void testDoGet() throws Exception {
        when(request.getRequestDispatcher("/WEB-INF/views/login.jsp")).thenReturn(dispatcher);

        loginServlet.doGet(request, response);

        verify(dispatcher, times(1)).forward(request, response);
    }

    @Test
    @DisplayName("测试POST请求 - 登录成功")
    void testDoPost_Success() throws Exception {
        when(request.getParameter("account")).thenReturn("testuser");
        when(request.getParameter("password")).thenReturn("password123");
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn("/forum");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setStatus("ACTIVE");
        when(userService.login(anyString(), anyString())).thenReturn(mockUser);

        loginServlet.doPost(request, response);

        verify(session, times(1)).setAttribute("currentUser", mockUser);
        verify(response, times(1)).sendRedirect("/forum/");
    }

    @Test
    @DisplayName("测试POST请求 - 密码错误")
    void testDoPost_WrongPassword() throws Exception {
        when(request.getParameter("account")).thenReturn("testuser");
        when(request.getParameter("password")).thenReturn("wrongpassword");
        when(request.getRequestDispatcher("/WEB-INF/views/login.jsp")).thenReturn(dispatcher);

        when(userService.login(anyString(), anyString()))
            .thenThrow(new RuntimeException("账号或密码不正确"));

        loginServlet.doPost(request, response);

        verify(request, times(1)).setAttribute(eq("error"), contains("密码"));
        verify(dispatcher, times(1)).forward(request, response);
    }

    @Test
    @DisplayName("测试POST请求 - 账号不存在")
    void testDoPost_UserNotFound() throws Exception {
        when(request.getParameter("account")).thenReturn("nonexistent");
        when(request.getParameter("password")).thenReturn("password123");
        when(request.getRequestDispatcher("/WEB-INF/views/login.jsp")).thenReturn(dispatcher);

        when(userService.login(anyString(), anyString()))
            .thenThrow(new RuntimeException("账号或密码不正确"));

        loginServlet.doPost(request, response);

        verify(request, times(1)).setAttribute(eq("error"), anyString());
        verify(dispatcher, times(1)).forward(request, response);
    }

    @Test
    @DisplayName("测试POST请求 - 账号已禁用")
    void testDoPost_AccountSuspended() throws Exception {
        when(request.getParameter("account")).thenReturn("suspendeduser");
        when(request.getParameter("password")).thenReturn("password123");
        when(request.getRequestDispatcher("/WEB-INF/views/login.jsp")).thenReturn(dispatcher);

        when(userService.login(anyString(), anyString()))
            .thenThrow(new RuntimeException("账号当前不可用"));

        loginServlet.doPost(request, response);

        verify(request, times(1)).setAttribute(eq("error"), contains("不可用"));
        verify(dispatcher, times(1)).forward(request, response);
    }
}
