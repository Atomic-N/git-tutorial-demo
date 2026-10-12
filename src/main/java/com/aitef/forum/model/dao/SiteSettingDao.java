package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.SiteSetting;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SiteSettingDao {
    public List<SiteSetting> findAll() {
        String sql = "SELECT * FROM site_settings ORDER BY setting_key ASC";
        List<SiteSetting> settings = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                settings.add(map(rs));
            }
            return settings;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load site settings", e);
        }
    }

    public Optional<SiteSetting> findByKey(String key) {
        String sql = "SELECT * FROM site_settings WHERE setting_key = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find site setting", e);
        }
    }

    public void save(Connection conn, String key, String value) {
        String sql = """
                INSERT INTO site_settings (setting_key, setting_value)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save site setting", e);
        }
    }

    private SiteSetting map(ResultSet rs) throws Exception {
        SiteSetting setting = new SiteSetting();
        setting.setSettingKey(rs.getString("setting_key"));
        setting.setSettingValue(rs.getString("setting_value"));
        setting.setUpdatedAt(DaoMapper.localDateTime(rs, "updated_at"));
        return setting;
    }
}
