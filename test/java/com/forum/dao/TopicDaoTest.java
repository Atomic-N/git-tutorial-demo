package com.forum.dao;

import com.forum.model.Topic;
import com.forum.util.DatabaseUtil;
import com.forum.dto.TopicSearchCriteria;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TopicDao数据访问层测试
 * 测试话题数据库操作
 */
@DisplayName("TopicDao测试")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TopicDaoTest {

    private TopicDao topicDao;
    private Connection connection;

    @BeforeAll
    void setUpDatabase() throws Exception {
        connection = DatabaseUtil.getConnection();
        topicDao = new TopicDao();
    }

    @BeforeEach
    void setUp() throws Exception {
        // 清理测试数据
        connection.createStatement().execute("DELETE FROM topics WHERE title LIKE 'Test Topic%'");
    }

    @AfterAll
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("测试创建话题")
    void testCreateTopic() throws Exception {
        Topic topic = createTestTopic("Test Topic 001", 1L, 1L);

        Long topicId = topicDao.create(connection, topic);

        assertNotNull(topicId);
        assertTrue(topicId > 0);
    }

    @Test
    @DisplayName("测试根据ID查找话题")
    void testFindById() throws Exception {
        Topic topic = createTestTopic("Test Topic 002", 1L, 1L);
        Long topicId = topicDao.create(connection, topic);

        Optional<Topic> found = topicDao.findById(topicId);

        assertTrue(found.isPresent());
        assertEquals("Test Topic 002", found.get().getTitle());
    }

    @Test
    @DisplayName("测试更新话题状态")
    void testUpdateStatus() throws Exception {
        Topic topic = createTestTopic("Test Topic 003", 1L, 1L);
        Long topicId = topicDao.create(connection, topic);

        topicDao.updateStatus(connection, topicId, "DELETED");

        Optional<Topic> updated = topicDao.findById(topicId);
        assertTrue(updated.isPresent());
        assertEquals("DELETED", updated.get().getStatus());
    }

    @Test
    @DisplayName("测试增加话题浏览量")
    void testIncrementViewCount() throws Exception {
        Topic topic = createTestTopic("Test Topic 004", 1L, 1L);
        Long topicId = topicDao.create(connection, topic);

        topicDao.incrementViewCount(connection, topicId);

        Optional<Topic> updated = topicDao.findById(topicId);
        assertTrue(updated.isPresent());
        assertEquals(1, updated.get().getViewCount());
    }

    @Test
    @DisplayName("测试增加话题回复数")
    void testIncrementReplyCount() throws Exception {
        Topic topic = createTestTopic("Test Topic 005", 1L, 1L);
        Long topicId = topicDao.create(connection, topic);

        topicDao.incrementReplyCount(connection, topicId);

        Optional<Topic> updated = topicDao.findById(topicId);
        assertTrue(updated.isPresent());
        assertEquals(1, updated.get().getReplyCount());
    }

    @Test
    @DisplayName("测试增加话题点赞数")
    void testIncrementLikeCount() throws Exception {
        Topic topic = createTestTopic("Test Topic 006", 1L, 1L);
        Long topicId = topicDao.create(connection, topic);

        topicDao.incrementLikeCount(connection, topicId);

        Optional<Topic> updated = topicDao.findById(topicId);
        assertTrue(updated.isPresent());
        assertEquals(1, updated.get().getLikeCount());
    }

    @Test
    @DisplayName("测试减少话题点赞数")
    void testDecrementLikeCount() throws Exception {
        Topic topic = createTestTopic("Test Topic 007", 1L, 1L);
        Long topicId = topicDao.create(connection, topic);

        topicDao.incrementLikeCount(connection, topicId);
        topicDao.decrementLikeCount(connection, topicId);

        Optional<Topic> updated = topicDao.findById(topicId);
        assertTrue(updated.isPresent());
        assertEquals(0, updated.get().getLikeCount());
    }

    @Test
    @DisplayName("测试增加话题收藏数")
    void testIncrementBookmarkCount() throws Exception {
        Topic topic = createTestTopic("Test Topic 008", 1L, 1L);
        Long topicId = topicDao.create(connection, topic);

        topicDao.incrementBookmarkCount(connection, topicId);

        Optional<Topic> updated = topicDao.findById(topicId);
        assertTrue(updated.isPresent());
        assertEquals(1, updated.get().getBookmarkCount());
    }

    @Test
    @DisplayName("测试减少话题收藏数")
    void testDecrementBookmarkCount() throws Exception {
        Topic topic = createTestTopic("Test Topic 009", 1L, 1L);
        Long topicId = topicDao.create(connection, topic);

        topicDao.incrementBookmarkCount(connection, topicId);
        topicDao.decrementBookmarkCount(connection, topicId);

        Optional<Topic> updated = topicDao.findById(topicId);
        assertTrue(updated.isPresent());
        assertEquals(0, updated.get().getBookmarkCount());
    }

    @Test
    @DisplayName("测试设置话题置顶")
    void testUpdatePinned() throws Exception {
        Topic topic = createTestTopic("Test Topic 010", 1L, 1L);
        Long topicId = topicDao.create(connection, topic);

        topicDao.updatePinned(connection, topicId, true);

        Optional<Topic> updated = topicDao.findById(topicId);
        assertTrue(updated.isPresent());
        assertTrue(updated.get().isPinned());
    }

    @Test
    @DisplayName("测试设置话题已解决")
    void testUpdateSolved() throws Exception {
        Topic topic = createTestTopic("Test Topic 011", 1L, 1L);
        Long topicId = topicDao.create(connection, topic);

        topicDao.updateSolved(connection, topicId, true, 1L);

        Optional<Topic> updated = topicDao.findById(topicId);
        assertTrue(updated.isPresent());
        assertTrue(updated.get().isSolved());
        assertEquals(1L, updated.get().getAcceptedPostId());
    }

    @Test
    @DisplayName("测试搜索话题 - 按关键词")
    void testSearchByKeyword() throws Exception {
        Topic topic1 = createTestTopic("Test Topic AI机器学习", 1L, 1L);
        Topic topic2 = createTestTopic("Test Topic 深度学习", 1L, 1L);
        topicDao.create(connection, topic1);
        topicDao.create(connection, topic2);

        TopicSearchCriteria criteria = new TopicSearchCriteria();
        criteria.setKeyword("学习");

        List<Topic> results = topicDao.search(connection, criteria, 0, 20);

        assertNotNull(results);
        assertTrue(results.size() >= 2);
    }

    @Test
    @DisplayName("测试搜索话题 - 按分类")
    void testSearchByCategory() throws Exception {
        Topic topic = createTestTopic("Test Topic 012", 1L, 5L);
        topicDao.create(connection, topic);

        TopicSearchCriteria criteria = new TopicSearchCriteria();
        criteria.setCategoryId(5L);

        List<Topic> results = topicDao.search(connection, criteria, 0, 20);

        assertNotNull(results);
        assertTrue(results.stream().allMatch(t -> t.getCategoryId().equals(5L)));
    }

    @Test
    @DisplayName("测试查询话题总数")
    void testCount() throws Exception {
        long countBefore = topicDao.count();

        Topic topic1 = createTestTopic("Test Topic 013", 1L, 1L);
        Topic topic2 = createTestTopic("Test Topic 014", 1L, 1L);
        topicDao.create(connection, topic1);
        topicDao.create(connection, topic2);

        long countAfter = topicDao.count();

        assertEquals(countBefore + 2, countAfter);
    }

    @Test
    @DisplayName("测试查询近期话题")
    void testFindRecent() throws Exception {
        Topic topic1 = createTestTopic("Test Topic 015", 1L, 1L);
        Topic topic2 = createTestTopic("Test Topic 016", 1L, 1L);
        topicDao.create(connection, topic1);
        topicDao.create(connection, topic2);

        List<Topic> recentTopics = topicDao.findRecent(0, 10, null);

        assertNotNull(recentTopics);
        assertTrue(recentTopics.size() >= 2);
    }

    // 辅助方法
    private Topic createTestTopic(String title, Long userId, Long categoryId) {
        Topic topic = new Topic();
        topic.setTitle(title);
        topic.setSlug(title.toLowerCase().replace(" ", "-"));
        topic.setUserId(userId);
        topic.setCategoryId(categoryId);
        topic.setExcerpt("这是测试话题的摘要");
        topic.setViewCount(0);
        topic.setReplyCount(0);
        topic.setLikeCount(0);
        topic.setBookmarkCount(0);
        topic.setStatus("ACTIVE");
        topic.setPinned(false);
        topic.setSolved(false);
        return topic;
    }
}
