package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.ChatChannelDao;
import com.aitef.forum.model.dao.ChatMessageDao;
import com.aitef.forum.model.entity.ChatChannel;
import com.aitef.forum.model.entity.ChatMessage;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.util.Database;
import com.aitef.forum.util.HtmlUtil;
import com.aitef.forum.util.ValidationException;

import java.sql.Connection;
import java.util.List;

public class ChatService {
    private final ChatChannelDao chatChannelDao = new ChatChannelDao();
    private final ChatMessageDao chatMessageDao = new ChatMessageDao();
    private final SiteSettingService siteSettingService = new SiteSettingService();

    public List<ChatChannel> listChannels(User user) {
        return chatChannelDao.listVisible(user == null ? 0 : user.getTrustLevel());
    }

    public ChatChannel getChannel(String slug, User user) {
        ChatChannel channel = chatChannelDao.findBySlug(slug).orElseThrow(() -> new ValidationException("频道不存在"));
        if (!canAccess(channel, user)) {
            throw new ValidationException("当前信任等级无法访问该频道");
        }
        return channel;
    }

    public List<ChatMessage> listMessages(Long channelId) {
        return chatMessageDao.listByChannelId(channelId, 120);
    }

    public Long sendMessage(User user, String channelSlug, Long replyToMessageId, String content) {
        if (user == null) {
            throw new ValidationException("请先登录");
        }
        String raw = content == null ? "" : content.trim();
        if (raw.length() < 2 || raw.length() > 4000) {
            throw new ValidationException("聊天消息长度需要在 2-4000 个字符之间");
        }
        ChatChannel channel = getChannel(channelSlug, user);
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (replyToMessageId != null) {
                    ChatMessage replyTo = chatMessageDao.findById(conn, replyToMessageId);
                    if (replyTo == null || !channel.getId().equals(replyTo.getChannelId())) {
                        throw new ValidationException("引用的消息不存在");
                    }
                }
                Long messageId = chatMessageDao.create(conn, channel.getId(), user.getId(), replyToMessageId, raw, cooked(raw));
                chatChannelDao.touch(conn, channel.getId());
                conn.commit();
                return messageId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send chat message", e);
        }
    }

    private boolean canAccess(ChatChannel channel, User user) {
        return (user == null ? 0 : user.getTrustLevel()) >= channel.getMinTrustLevel() || (user != null && user.isAdmin());
    }

    private String cooked(String raw) {
        return siteSettingService.booleanValue("markdown_enabled")
                ? HtmlUtil.toCookedContent(raw)
                : "<p>" + HtmlUtil.escape(raw).replace("\n", "<br>") + "</p>";
    }
}
