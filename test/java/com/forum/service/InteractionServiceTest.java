package com.forum.service;

import com.forum.dao.*;
import com.forum.model.Reaction;
import com.forum.model.Bookmark;
import com.forum.model.Topic;
import com.forum.model.Post;
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
 * 互动服务测试类
 * 测试点赞、收藏等互动功能
 */
@DisplayName("互动服务测试")
public class InteractionServiceTest {

    private InteractionService interactionService;

    @Mock
    private ReactionDao reactionDao;

    @Mock
    private BookmarkDao bookmarkDao;

    @Mock
    private TopicDao topicDao;

    @Mock
    private PostDao postDao;

    @Mock
    private Connection connection;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interactionService = new InteractionService(reactionDao, bookmarkDao, topicDao, postDao);
    }

    @Test
    @DisplayName("测试用例1：点击话题点赞按钮")
    void testToggleTopicLike_AddLike() throws Exception {
        Long userId = 1L;
        Long topicId = 1L;

        when(reactionDao.findByUserAndTarget(userId, "TOPIC", topicId)).thenReturn(Optional.empty());
        when(topicDao.findById(topicId)).thenReturn(Optional.of(createMockTopic(topicId)));

        boolean result = interactionService.toggleTopicLike(userId, topicId);

        assertTrue(result, "应该返回true表示已点赞");
        verify(reactionDao, times(1)).create(any(Connection.class), any(Reaction.class));
        verify(topicDao, times(1)).incrementLikeCount(any(Connection.class), eq(topicId));
    }

    @Test
    @DisplayName("测试用例2：再次点击已点赞的话题")
    void testToggleTopicLike_RemoveLike() throws Exception {
        Long userId = 1L;
        Long topicId = 1L;

        Reaction existingReaction = new Reaction();
        existingReaction.setUserId(userId);
        existingReaction.setTargetType("TOPIC");
        existingReaction.setTargetId(topicId);

        when(reactionDao.findByUserAndTarget(userId, "TOPIC", topicId))
            .thenReturn(Optional.of(existingReaction));
        when(topicDao.findById(topicId)).thenReturn(Optional.of(createMockTopic(topicId)));

        boolean result = interactionService.toggleTopicLike(userId, topicId);

        assertFalse(result, "应该返回false表示已取消点赞");
        verify(reactionDao, times(1)).delete(any(Connection.class), eq(userId), eq("TOPIC"), eq(topicId));
        verify(topicDao, times(1)).decrementLikeCount(any(Connection.class), eq(topicId));
    }

    @Test
    @DisplayName("测试用例3：点击话题收藏按钮")
    void testToggleTopicBookmark_AddBookmark() throws Exception {
        Long userId = 1L;
        Long topicId = 1L;

        when(bookmarkDao.findByUserAndTarget(userId, "TOPIC", topicId)).thenReturn(Optional.empty());
        when(topicDao.findById(topicId)).thenReturn(Optional.of(createMockTopic(topicId)));

        boolean result = interactionService.toggleTopicBookmark(userId, topicId);

        assertTrue(result, "应该返回true表示已收藏");
        verify(bookmarkDao, times(1)).create(any(Connection.class), any(Bookmark.class));
        verify(topicDao, times(1)).incrementBookmarkCount(any(Connection.class), eq(topicId));
    }

    @Test
    @DisplayName("测试用例4：在个人书签页取消收藏")
    void testToggleTopicBookmark_RemoveBookmark() throws Exception {
        Long userId = 1L;
        Long topicId = 1L;

        Bookmark existingBookmark = new Bookmark();
        existingBookmark.setUserId(userId);
        existingBookmark.setTargetType("TOPIC");
        existingBookmark.setTargetId(topicId);

        when(bookmarkDao.findByUserAndTarget(userId, "TOPIC", topicId))
            .thenReturn(Optional.of(existingBookmark));
        when(topicDao.findById(topicId)).thenReturn(Optional.of(createMockTopic(topicId)));

        boolean result = interactionService.toggleTopicBookmark(userId, topicId);

        assertFalse(result, "应该返回false表示已取消收藏");
        verify(bookmarkDao, times(1)).delete(any(Connection.class), eq(userId), eq("TOPIC"), eq(topicId));
        verify(topicDao, times(1)).decrementBookmarkCount(any(Connection.class), eq(topicId));
    }

    @Test
    @DisplayName("测试用例5：对回复内容点赞")
    void testTogglePostLike_AddLike() throws Exception {
        Long userId = 1L;
        Long postId = 1L;

        when(reactionDao.findByUserAndTarget(userId, "POST", postId)).thenReturn(Optional.empty());
        when(postDao.findById(postId)).thenReturn(Optional.of(createMockPost(postId)));

        boolean result = interactionService.togglePostLike(userId, postId);

        assertTrue(result, "应该返回true表示已点赞回复");
        verify(reactionDao, times(1)).create(any(Connection.class), any(Reaction.class));
        verify(postDao, times(1)).incrementLikeCount(any(Connection.class), eq(postId));
    }

    @Test
    @DisplayName("测试用例6：对回复取消点赞")
    void testTogglePostLike_RemoveLike() throws Exception {
        Long userId = 1L;
        Long postId = 1L;

        Reaction existingReaction = new Reaction();
        existingReaction.setUserId(userId);
        existingReaction.setTargetType("POST");
        existingReaction.setTargetId(postId);

        when(reactionDao.findByUserAndTarget(userId, "POST", postId))
            .thenReturn(Optional.of(existingReaction));
        when(postDao.findById(postId)).thenReturn(Optional.of(createMockPost(postId)));

        boolean result = interactionService.togglePostLike(userId, postId);

        assertFalse(result, "应该返回false表示已取消点赞回复");
        verify(reactionDao, times(1)).delete(any(Connection.class), eq(userId), eq("POST"), eq(postId));
        verify(postDao, times(1)).decrementLikeCount(any(Connection.class), eq(postId));
    }

    @Test
    @DisplayName("测试用例7：点赞不存在的话题")
    void testToggleTopicLike_TopicNotFound() {
        Long userId = 1L;
        Long topicId = 999L;

        when(reactionDao.findByUserAndTarget(userId, "TOPIC", topicId)).thenReturn(Optional.empty());
        when(topicDao.findById(topicId)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> {
            interactionService.toggleTopicLike(userId, topicId);
        });
    }

    @Test
    @DisplayName("测试用例8：收藏不存在的话题")
    void testToggleTopicBookmark_TopicNotFound() {
        Long userId = 1L;
        Long topicId = 999L;

        when(bookmarkDao.findByUserAndTarget(userId, "TOPIC", topicId)).thenReturn(Optional.empty());
        when(topicDao.findById(topicId)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> {
            interactionService.toggleTopicBookmark(userId, topicId);
        });
    }

    @Test
    @DisplayName("测试用例9：并发点赞测试")
    void testConcurrentLikes() throws Exception {
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long topicId = 1L;

        when(reactionDao.findByUserAndTarget(userId1, "TOPIC", topicId)).thenReturn(Optional.empty());
        when(reactionDao.findByUserAndTarget(userId2, "TOPIC", topicId)).thenReturn(Optional.empty());
        when(topicDao.findById(topicId)).thenReturn(Optional.of(createMockTopic(topicId)));

        // 模拟两个用户同时点赞
        boolean result1 = interactionService.toggleTopicLike(userId1, topicId);
        boolean result2 = interactionService.toggleTopicLike(userId2, topicId);

        assertTrue(result1);
        assertTrue(result2);
        verify(reactionDao, times(2)).create(any(Connection.class), any(Reaction.class));
        verify(topicDao, times(2)).incrementLikeCount(any(Connection.class), eq(topicId));
    }

    // 辅助方法
    private Topic createMockTopic(Long id) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setTitle("Test Topic");
        topic.setUserId(1L);
        topic.setCategoryId(1L);
        topic.setLikeCount(0);
        topic.setBookmarkCount(0);
        topic.setStatus("ACTIVE");
        return topic;
    }

    private Post createMockPost(Long id) {
        Post post = new Post();
        post.setId(id);
        post.setTopicId(1L);
        post.setUserId(1L);
        post.setRawContent("Test content");
        post.setCookedContent("Test content");
        post.setPostNumber(1);
        post.setLikeCount(0);
        return post;
    }
}
