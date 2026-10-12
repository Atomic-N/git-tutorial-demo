package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.PostDao;
import com.aitef.forum.model.dao.ReportDao;
import com.aitef.forum.model.dao.TopicDao;
import com.aitef.forum.model.dto.Page;
import com.aitef.forum.model.dto.TopicCard;
import com.aitef.forum.model.entity.Post;
import com.aitef.forum.model.entity.Report;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.util.Database;
import com.aitef.forum.util.ValidationException;

import java.sql.Connection;

public class ReportService {
    private final ReportDao reportDao = new ReportDao();
    private final TopicDao topicDao = new TopicDao();
    private final PostDao postDao = new PostDao();
    private final AuditLogService auditLogService = new AuditLogService();

    public Long createReport(User reporter, String targetType, Long targetId, String reason, String detail) {
        if (reporter == null) {
            throw new ValidationException("请先登录");
        }
        targetType = normalize(targetType).toUpperCase();
        reason = normalize(reason);
        detail = normalize(detail);
        if (!"TOPIC".equals(targetType) && !"POST".equals(targetType)) {
            throw new ValidationException("举报对象不正确");
        }
        if (targetId == null) {
            throw new ValidationException("举报对象不存在");
        }
        if (reason.length() < 2 || reason.length() > 80) {
            throw new ValidationException("请选择或填写举报原因");
        }
        if ("TOPIC".equals(targetType)) {
            topicDao.findCardById(targetId, reporter.getId()).orElseThrow(() -> new ValidationException("话题不存在"));
        } else {
            postDao.findById(targetId).orElseThrow(() -> new ValidationException("回复不存在"));
        }
        Long reportId = reportDao.create(reporter.getId(), targetType, targetId, reason, detail.length() > 1000 ? detail.substring(0, 1000) : detail);
        auditLogService.log(reporter.getId(), "CREATE_REPORT", targetType, targetId, "reportId=" + reportId + ",reason=" + reason);
        return reportId;
    }

    public Page<Report> listReports(String status, int page, int size) {
        return reportDao.list(status, page, size);
    }

    public void updateStatus(Long actorId, Long reportId, String status) {
        if (reportId == null) {
            throw new ValidationException("举报不存在");
        }
        status = normalizeStatus(status);
        reportDao.updateStatus(reportId, status);
        auditLogService.log(actorId, "REPORT_STATUS_UPDATE", "REPORT", reportId, "status=" + status);
    }

    public void resolveAndHide(Long actorId, Long reportId) {
        if (reportId == null) {
            throw new ValidationException("举报不存在");
        }
        Report report = reportDao.findById(reportId)
                .orElseThrow(() -> new ValidationException("举报不存在"));
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if ("TOPIC".equalsIgnoreCase(report.getTargetType())) {
                    TopicCard topic = topicDao.findCardById(report.getTargetId(), null, 99)
                            .orElseThrow(() -> new ValidationException("话题不存在"));
                    topicDao.softDelete(conn, report.getTargetId());
                    topicDao.refreshCategoryCount(conn, topic.getCategoryId());
                } else if ("POST".equalsIgnoreCase(report.getTargetType())) {
                    Post post = postDao.findById(report.getTargetId())
                            .orElseThrow(() -> new ValidationException("回复不存在"));
                    TopicCard topic = topicDao.findCardById(post.getTopicId(), null, 99)
                            .orElseThrow(() -> new ValidationException("话题不存在"));
                    postDao.softDelete(conn, report.getTargetId());
                    topicDao.refreshTopicAfterPostChange(conn, post.getTopicId());
                    topicDao.refreshCategoryCount(conn, topic.getCategoryId());
                } else {
                    throw new ValidationException("举报对象不正确");
                }
                reportDao.updateStatus(conn, reportId, "RESOLVED");
                auditLogService.log(conn, actorId, "REPORT_RESOLVE_AND_HIDE", "REPORT", reportId, report.getTargetType() + ":" + report.getTargetId());
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve report", e);
        }
    }

    private String normalizeStatus(String status) {
        status = normalize(status).toUpperCase();
        if (!"PENDING".equals(status) && !"RESOLVED".equals(status) && !"REJECTED".equals(status)) {
            throw new ValidationException("处理状态不正确");
        }
        return status;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
