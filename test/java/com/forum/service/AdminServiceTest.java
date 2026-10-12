package com.forum.service;

import com.forum.dao.*;
import com.forum.model.User;
import com.forum.model.Topic;
import com.forum.model.Report;
import com.forum.exception.ValidationException;
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
 * 后台管理服务测试类
 * 测试用户管理、内容治理等后台功能
 */
@DisplayName("后台管理服务测试")
public class AdminServiceTest {

    private AdminService adminService;

    @Mock
    private UserDao userDao;

    @Mock
    private TopicDao topicDao;

    @Mock
    private PostDao postDao;

    @Mock
    private ReportDao reportDao;

    @Mock
    private RoleDao roleDao;

    @Mock
    private Connection connection;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminService = new AdminService(userDao, topicDao, postDao, reportDao, roleDao);
    }

    @Test
    @DisplayName("测试用例1：普通用户访问后台地址")
    void testRequireModeratorWithNormalUser() {
        User normalUser = createMockUser(1L, "normaluser", "USER");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            adminService.requireModerator(normalUser);
        });
        assertTrue(exception.getMessage().contains("权限") || exception.getMessage().contains("无权"));
    }

    @Test
    @DisplayName("测试用例2：版主访问后台")
    void testRequireModeratorWithModerator() {
        User moderator = createMockUser(2L, "moderator", "MODERATOR");

        assertDoesNotThrow(() -> {
            adminService.requireModerator(moderator);
        });
    }

    @Test
    @DisplayName("测试用例3：管理员访问后台")
    void testRequireModeratorWithAdmin() {
        User admin = createMockUser(3L, "admin", "ADMIN");

        assertDoesNotThrow(() -> {
            adminService.requireModerator(admin);
        });
    }

    @Test
    @DisplayName("测试用例4：管理员删除违规话题")
    void testDeleteTopic() throws Exception {
        Long adminId = 1L;
        Long topicId = 100L;

        Topic mockTopic = createMockTopic(topicId, "违规话题");
        when(topicDao.findById(topicId)).thenReturn(Optional.of(mockTopic));

        adminService.deleteTopic(adminId, topicId);

        verify(topicDao, times(1)).updateStatus(any(Connection.class), eq(topicId), eq("DELETED"));
    }

    @Test
    @DisplayName("测试用例5：为用户分配版主角色")
    void testAssignModeratorRole() throws Exception {
        Long adminId = 1L;
        Long userId = 10L;
        Long moderatorRoleId = 2L;

        User mockUser = createMockUser(userId, "targetuser", "USER");
        when(userDao.findById(userId)).thenReturn(Optional.of(mockUser));
        when(roleDao.findByCode("MODERATOR")).thenReturn(Optional.of(createMockRole(moderatorRoleId, "MODERATOR")));

        adminService.assignRole(adminId, userId, "MODERATOR");

        verify(roleDao, times(1)).assignRoleToUser(any(Connection.class), eq(userId), eq(moderatorRoleId));
    }

    @Test
    @DisplayName("测试用例6：处理用户举报")
    void testHandleReport() throws Exception {
        Long adminId = 1L;
        Long reportId = 50L;

        Report mockReport = createMockReport(reportId, "PENDING");
        when(reportDao.findById(reportId)).thenReturn(Optional.of(mockReport));

        adminService.updateReportStatus(adminId, reportId, "RESOLVED");

        verify(reportDao, times(1)).updateStatus(any(Connection.class), eq(reportId), eq("RESOLVED"));
    }

    @Test
    @DisplayName("测试用例7：修改站点配置")
    void testUpdateSiteSettings() throws Exception {
        Long adminId = 1L;
        String settingKey = "site_name";
        String settingValue = "AI技术交流论坛";

        adminService.updateSiteSetting(adminId, settingKey, settingValue);

        verify(connection, times(1)).prepareStatement(anyString());
    }

    @Test
    @DisplayName("测试用例8：查看近期用户列表")
    void testGetRecentUsers() {
        when(userDao.findRecent(anyInt())).thenReturn(java.util.Collections.emptyList());

        var users = adminService.recentUsers(16);

        assertNotNull(users);
        verify(userDao, times(1)).findRecent(16);
    }

    @Test
    @DisplayName("测试用例9：查看近期话题列表")
    void testGetRecentTopics() {
        when(topicDao.findRecent(anyInt(), anyInt(), anyLong())).thenReturn(java.util.Collections.emptyList());

        var topics = adminService.recentTopics(1, 1L);

        assertNotNull(topics);
        verify(topicDao, times(1)).findRecent(anyInt(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("测试用例10：更新用户状态为禁用")
    void testSuspendUser() throws Exception {
        Long adminId = 1L;
        Long userId = 10L;

        User mockUser = createMockUser(userId, "baduser", "USER");
        when(userDao.findById(userId)).thenReturn(Optional.of(mockUser));

        adminService.updateUserStatus(adminId, userId, "SUSPENDED");

        verify(userDao, times(1)).updateStatus(any(Connection.class), eq(userId), eq("SUSPENDED"));
    }

    @Test
    @DisplayName("测试用例11：更新用户信任等级")
    void testUpdateUserTrustLevel() throws Exception {
        Long adminId = 1L;
        Long userId = 10L;
        int newTrustLevel = 3;

        User mockUser = createMockUser(userId, "gooduser", "USER");
        when(userDao.findById(userId)).thenReturn(Optional.of(mockUser));

        adminService.updateUserTrustLevel(adminId, userId, newTrustLevel);

        verify(userDao, times(1)).updateTrustLevel(any(Connection.class), eq(userId), eq(newTrustLevel));
    }

    @Test
    @DisplayName("测试用例12：删除违规回复")
    void testDeletePost() throws Exception {
        Long adminId = 1L;
        Long postId = 200L;

        adminService.deletePost(adminId, postId);

        verify(postDao, times(1)).updateStatus(any(Connection.class), eq(postId), eq("DELETED"));
    }

    @Test
    @DisplayName("测试用例13：置顶话题")
    void testPinTopic() throws Exception {
        Long adminId = 1L;
        Long topicId = 100L;

        Topic mockTopic = createMockTopic(topicId, "重要话题");
        when(topicDao.findById(topicId)).thenReturn(Optional.of(mockTopic));

        adminService.pinTopic(adminId, topicId, true);

        verify(topicDao, times(1)).updatePinned(any(Connection.class), eq(topicId), eq(true));
    }

    @Test
    @DisplayName("测试用例14：取消置顶话题")
    void testUnpinTopic() throws Exception {
        Long adminId = 1L;
        Long topicId = 100L;

        Topic mockTopic = createMockTopic(topicId, "普通话题");
        when(topicDao.findById(topicId)).thenReturn(Optional.of(mockTopic));

        adminService.pinTopic(adminId, topicId, false);

        verify(topicDao, times(1)).updatePinned(any(Connection.class), eq(topicId), eq(false));
    }

    @Test
    @DisplayName("测试用例15：查看系统统计数据")
    void testGetSystemStats() {
        when(userDao.count()).thenReturn(1000L);
        when(topicDao.count()).thenReturn(5000L);

        var stats = adminService.stats();

        assertNotNull(stats);
        verify(userDao, times(1)).count();
        verify(topicDao, times(1)).count();
    }

    // 辅助方法
    private User createMockUser(Long id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setTrustLevel(1);
        return user;
    }

    private Topic createMockTopic(Long id, String title) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setTitle(title);
        topic.setUserId(1L);
        topic.setCategoryId(1L);
        topic.setStatus("ACTIVE");
        topic.setPinned(false);
        return topic;
    }

    private Report createMockReport(Long id, String status) {
        Report report = new Report();
        report.setId(id);
        report.setReporterId(1L);
        report.setTargetType("TOPIC");
        report.setTargetId(100L);
        report.setReason("违规内容");
        report.setStatus(status);
        return report;
    }

    private Object createMockRole(Long id, String code) {
        // 简化的Role对象
        return new Object();
    }
}
