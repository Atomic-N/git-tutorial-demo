package com.forum.servlet;

import com.forum.model.User;
import com.forum.service.TopicService;
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
 * 话题Servlet测试
 * 测试话题发布和回复请求处理
 */
@DisplayName("话题Servlet测试")
public class TopicServletTest {

    private TopicServlet topicServlet;

    @Mock
    private TopicService topicService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private RequestDispatcher dispatcher;

    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        topicServlet = new TopicServlet();
        topicServlet.setTopicService(topicService);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("测试未登录发布话题")
    void testCreateTopic_NotLoggedIn() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/forum");

        topicServlet.doPost(request, response);

        verify(response, times(1)).sendRedirect(contains("login"));
    }

    @Test
    @DisplayName("测试发布话题 - 成功")
    void testCreateTopic_Success() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("currentUser")).thenReturn(mockUser);
        when(request.getParameter("action")).thenReturn("create");
        when(request.getParameter("title")).thenReturn("测试话题标题");
        when(request.getParameter("categoryId")).thenReturn("1");
        when(request.getParameter("content")).thenReturn("这是话题内容");
        when(request.getParameter("tags")).thenReturn("AI,机器学习");
        when(request.getContextPath()).thenReturn("/forum");

        when(topicService.createTopic(anyLong(), anyLong(), anyString(), anyString(), anyString()))
            .thenReturn(100L);

        topicServlet.doPost(request, response);

        verify(topicService, times(1)).createTopic(eq(1L), eq(1L), eq("测试话题标题"), eq("这是话题内容"), eq("AI,机器学习"));
        verify(response, times(1)).sendRedirect("/forum/t/100");
    }

    @Test
    @DisplayName("测试发布话题 - 标题为空")
    void testCreateTopic_EmptyTitle() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("currentUser")).thenReturn(mockUser);
        when(request.getParameter("action")).thenReturn("create");
        when(request.getParameter("title")).thenReturn("");
        when(request.getParameter("categoryId")).thenReturn("1");
        when(request.getParameter("content")).thenReturn("内容");
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);

        when(topicService.createTopic(anyLong(), anyLong(), anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("请输入话题标题"));

        topicServlet.doPost(request, response);

        verify(request, times(1)).setAttribute(eq("error"), contains("标题"));
        verify(dispatcher, times(1)).forward(request, response);
    }

    @Test
    @DisplayName("测试回复话题 - 成功")
    void testReplyTopic_Success() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("currentUser")).thenReturn(mockUser);
        when(request.getParameter("action")).thenReturn("reply");
        when(request.getParameter("topicId")).thenReturn("100");
        when(request.getParameter("content")).thenReturn("这是一条回复");
        when(request.getParameter("replyToPostId")).thenReturn(null);
        when(request.getContextPath()).thenReturn("/forum");

        when(topicService.reply(anyLong(), anyLong(), any(), anyString()))
            .thenReturn(200L);

        topicServlet.doPost(request, response);

        verify(topicService, times(1)).reply(eq(1L), eq(100L), isNull(), eq("这是一条回复"));
        verify(response, times(1)).sendRedirect(contains("/t/100"));
    }

    @Test
    @DisplayName("测试引用回复 - 成功")
    void testQuoteReply_Success() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("currentUser")).thenReturn(mockUser);
        when(request.getParameter("action")).thenReturn("reply");
        when(request.getParameter("topicId")).thenReturn("100");
        when(request.getParameter("content")).thenReturn("引用回复内容");
        when(request.getParameter("replyToPostId")).thenReturn("50");
        when(request.getContextPath()).thenReturn("/forum");

        when(topicService.reply(anyLong(), anyLong(), anyLong(), anyString()))
            .thenReturn(201L);

        topicServlet.doPost(request, response);

        verify(topicService, times(1)).reply(eq(1L), eq(100L), eq(50L), eq("引用回复内容"));
        verify(response, times(1)).sendRedirect(contains("/t/100"));
    }

    @Test
    @DisplayName("测试查看话题详情")
    void testViewTopic() throws Exception {
        when(request.getParameter("id")).thenReturn("100");
        when(request.getRequestDispatcher("/WEB-INF/views/topic-detail.jsp")).thenReturn(dispatcher);

        topicServlet.doGet(request, response);

        verify(topicService, times(1)).getTopicDetail(eq(100L), any());
        verify(dispatcher, times(1)).forward(request, response);
    }
}
