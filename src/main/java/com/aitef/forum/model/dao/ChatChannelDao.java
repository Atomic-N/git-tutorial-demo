package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.ChatChannel;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChatChannelDao {
    public List<ChatChannel> listVisible(int trustLevel) {
        String sql = "SELECT * FROM chat_channels WHERE min_trust_level <= ? ORDER BY position ASC, id ASC";
        List<ChatChannel> channels = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trustLevel);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    channels.add(map(rs));
                }
            }
            return channels;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list chat channels", e);
        }
    }

    public Optional<ChatChannel> findBySlug(String slug) {
        String sql = "SELECT * FROM chat_channels WHERE slug = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find chat channel by slug", e);
        }
    }

    public Optional<ChatChannel> findById(Connection conn, Long id) {
        String sql = "SELECT * FROM chat_channels WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find chat channel by id", e);
        }
    }

    public void touch(Connection conn, Long channelId) {
        String sql = "UPDATE chat_channels SET message_count = message_count + 1, last_message_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update chat channel", e);
        }
    }

    private ChatChannel map(ResultSet rs) throws Exception {
        ChatChannel channel = new ChatChannel();
        channel.setId(rs.getLong("id"));
        channel.setName(rs.getString("name"));
        channel.setSlug(rs.getString("slug"));
        channel.setDescription(rs.getString("description"));
        channel.setIcon(rs.getString("icon"));
        channel.setPosition(rs.getInt("position"));
        channel.setMinTrustLevel(rs.getInt("min_trust_level"));
        channel.setMessageCount(rs.getInt("message_count"));
        channel.setLastMessageAt(DaoMapper.localDateTime(rs, "last_message_at"));
        return channel;
    }
}
