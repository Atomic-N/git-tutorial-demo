package com.forum.service;

import com.forum.dao.*;
import com.forum.model.Topic;
import com.forum.model.Post;
import com.forum.model.User;
import com.forum.exception.ValidationException;
import com.forum.dto.TopicSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 话题服务测试类
 * 测试话题发布、回复、搜索等核心功能
 */
@DisplayName("话题服务测试")
public class TopicServiceTest {

    private TopicService topicService;

    @Mock
    private TopicDao topicDao;

    @Mock
    private PostDao postDao;

    @Mock
    private UserDao userDao;

    @Mock
    private CategoryDao categoryDao;

    @Mock
    private TagDao tagDao;

    @Mock
    private Connection connection;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        topicService = new TopicService(topicDao, postDao, userDao, categoryDao, tagDao);
    }

    @Test
    @DisplayName("测试用例1：未登录状态发布话题")
    void testCreateTopicWithoutLogin() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            topicService.createTopic(null, 1L, "Test Topic", "Content", "tag1,tag2");
        });
        assertTrue(exception.getMessage().contains("登录") || exception.getMessage().contains("未授权"));
    }

    @Test
    @DisplayName("测试用例2：发布话题时标题为空")
    void testCreateTopicWithEmptyTitle() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            topicService.createTopic(1L, 1L, "", "Content", "tag1");
        });
        assertEquals("请输入话题标题", exception.getMessage());
    }

    @Test
    @DisplayName("测试用例3：发布话题时内容为空")
    void testCreateTopicWithEmptyContent() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            topicService.createTopic(1L, 1L, "Valid Title", "", "tag1");
        });
        assertTrue(exception.getMessage().contains("内容"));
    }

    @Test
    @DisplayName("测试用例4：标题长度超过200位")
    void testCreateTopicWithTitleTooLong() {
        String longTitle = "a".repeat(201);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            topicService.createTopic(1L, 1L, longTitle, "Content", "tag1");
        });
        assertTrue(exception.getMessage().contains("标题") && exception.getMessage().contains("200"));
    }

    @Test
    @DisplayName("测试用例5：填写完整标题、分类、正文后提交话题")
    void testCreateTopicSuccess() throws Exception {
        when(userDao.findById(1L)).thenReturn(Optional.of(createMockUser(1L, "testuser")));
        when(categoryDao.findById(1L)).thenReturn(Optional.of(createMockCategory(1L, "AI技术")));
        when(topicDao.create(any(Connection.class), any(Topic.class))).thenReturn(1L);
        when(postDao.create(any(Connection.class), any(Post.class))).thenReturn(1L);

        Long topicId = topicService.createTopic(1L, 1L, "测试话题标题", "这是测试内容", "AI,机器学习");

        assertNotNull(topicId);
        assertEquals(1L, topicId);
        verify(topicDao, times(1)).create(any(Connection.class), any(Topic.class));
        verify(postDao, times(1)).create(any(Connection.class), any(Post.class));
    }

    @Test
    @DisplayName("测试用例6：在话题详情页输入回复内容提交")
    void testReplyToTopicSuccess() throws Exception {
        Topic mockTopic = createMockTopic(1L, "Test Topic");
        when(topicDao.findById(1L)).thenReturn(Optional.of(mockTopic));
        when(userDao.findById(1L)).thenReturn(Optional.of(createMockUser(1L, "testuser")));
        when(postDao.create(any(Connection.class), any(Post.class))).thenReturn(2L);

        Long postId = topicService.reply(1L, 1L, null, "这是一条回复内容");

        assertNotNull(postId);
        assertEquals(2L, postId);
        verify(postDao, times(1)).create(any(Connection.class), any(Post.class));
        verify(topicDao, times(1)).incrementReplyCount(any(Connection.class), eq(1L));
    }

    @Test
    @DisplayName("测试用例7：引用某一楼层回复并提交")
    void testReplyWithQuote() throws Exception {
        Topic mockTopic = createMockTopic(1L, "Test Topic");
        Post mockReplyTo = createMockPost(2L, 1L, "被引用的内容");

        when(topicDao.findById(1L)).thenReturn(Optional.of(mockTopic));
        when(userDao.findById(1L)).thenReturn(Optional.of(createMockUser(1L, "testuser")));
        when(postDao.findById(2L)).thenReturn(Optional.of(mockReplyTo));
        when(postDao.create(any(Connection.class), any(Post.class))).thenReturn(3L);

        Long postId = topicService.reply(1L, 1L, 2L, "引用回复的内容");

        assertNotNull(postId);
        assertEquals(3L, postId);
        verify(postDao, times(1)).create(any(Connection.class), argThat(post ->
            post.getReplyToPostId() != null && post.getReplyToPostId().equals(2L)
        ));
    }

    @Test
    @DisplayName("测试用例8：搜索话题 - 按关键词")
    void testSearchTopicsByKeyword() {
        TopicSearchCriteria criteria = new TopicSearchCriteria();
        criteria.setKeyword("机器学习");
        criteria.setOrderBy("LATEST");

        topicService.list("latest", criteria, null, 1L);

        verify(topicDao, times(1)).search(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("测试用例9：搜索话题 - 按分类")
    void testSearchTopicsByCategory() {
        TopicSearchCriteria criteria = new TopicSearchCriteria();
        criteria.setCategoryId(1L);
        criteria.setOrderBy("LATEST");

        topicService.list("latest", criteria, null, 1L);

        verify(topicDao, times(1)).search(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("测试用例10：搜索话题 - 按标签")
    void testSearchTopicsByTag() {
        TopicSearchCriteria criteria = new TopicSearchCriteria();
        criteria.setTagId(1L);
        criteria.setOrderBy("LATEST");

        topicService.list("latest", criteria, null, 1L);

        verify(topicDao, times(1)).search(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("测试用例11：搜索话题 - 仅看已解决")
    void testSearchSolvedTopicsOnly() {
        TopicSearchCriteria criteria = new TopicSearchCriteria();
        criteria.setSolvedOnly(true);
        criteria.setOrderBy("LATEST");

        topicService.list("latest", criteria, null, 1L);

        verify(topicDao, times(1)).search(any(), any(), anyInt(), anyInt());
    }

    // 辅助方法
    private User createMockUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setStatus("ACTIVE");
        return user;
    }

    private Topic createMockTopic(Long id, String title) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setTitle(title);
        topic.setUserId(1L);
        topic.setCategoryId(1L);
        topic.setStatus("ACTIVE");
        return topic;
    }

    private Post createMockPost(Long id, Long topicId, String content) {
        Post post = new Post();
        post.setId(id);
        post.setTopicId(topicId);
        post.setUserId(1L);
        post.setRawContent(content);
        post.setCookedContent(content);
        post.setPostNumber(1);
        return post;
    }

    private Object createMockCategory(Long id, String name) {
        // 简化的Category对象创建
        return new Object();
    }
}
