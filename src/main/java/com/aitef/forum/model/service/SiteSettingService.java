package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.SiteSettingDao;
import com.aitef.forum.model.entity.SiteSetting;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SiteSettingService {
    private static final Map<String, String> DEFAULTS = Map.of(
            "site_name", "AI TEF",
            "notice_text", "真诚、友善、团结、专业，共建 AI 技术交流社区。",
            "community_guidelines_url", "/t/1/welcome-to-ai-tef",
            "allow_private_messages", "true",
            "markdown_enabled", "true"
    );

    private final SiteSettingDao siteSettingDao = new SiteSettingDao();
    private final AuditLogService auditLogService = new AuditLogService();

    public Map<String, String> allAsMap() {
        Map<String, String> settings = new LinkedHashMap<>(DEFAULTS);
        for (SiteSetting setting : siteSettingDao.findAll()) {
            settings.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return settings;
    }

    public List<SiteSetting> list() {
        return siteSettingDao.findAll();
    }

    public String value(String key) {
        return siteSettingDao.findByKey(key)
                .map(SiteSetting::getSettingValue)
                .orElse(DEFAULTS.getOrDefault(key, ""));
    }

    public boolean booleanValue(String key) {
        return !"false".equalsIgnoreCase(value(key));
    }

    public void saveAll(Long actorId, Map<String, String> updates) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (Map.Entry<String, String> entry : updates.entrySet()) {
                    siteSettingDao.save(conn, entry.getKey(), entry.getValue());
                }
                auditLogService.log(conn, actorId, "SITE_SETTING_UPDATE", "SITE_SETTING", null, String.join(",", updates.keySet()));
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save site settings", e);
        }
    }
}
