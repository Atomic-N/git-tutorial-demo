package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.NotificationDao;
import com.aitef.forum.model.dto.Page;
import com.aitef.forum.model.entity.Notification;

public class NotificationService {
    private final NotificationDao notificationDao = new NotificationDao();

    public Page<Notification> list(Long userId, int page, int size) {
        return notificationDao.listByUser(userId, page, size);
    }

    public int countUnread(Long userId) {
        return notificationDao.countUnread(userId);
    }

    public void markAllRead(Long userId) {
        notificationDao.markAllRead(userId);
    }
}
