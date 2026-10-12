package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.UserDao;
import com.aitef.forum.model.entity.User;

import java.sql.Connection;

public class TrustLevelService {
    private final UserDao userDao = new UserDao();

    public int refresh(Connection conn, Long userId) {
        User user = userDao.findById(conn, userId).orElse(null);
        if (user == null) {
            return 0;
        }
        int trustLevel = calculate(user);
        userDao.updateTrustLevel(conn, userId, trustLevel);
        return trustLevel;
    }

    public int calculate(User user) {
        if (user == null) {
            return 0;
        }
        if (user.isAdmin()) {
            return 4;
        }
        int posts = user.getPostsCount();
        int topics = user.getTopicsCount();
        int likes = user.getLikesReceivedCount();
        if (posts >= 60 && topics >= 15 && likes >= 40) {
            return 4;
        }
        if (posts >= 25 && topics >= 8 && likes >= 15) {
            return 3;
        }
        if (posts >= 10 && topics >= 3 && likes >= 5) {
            return 2;
        }
        if (posts >= 3 && topics >= 1 && likes >= 1) {
            return 1;
        }
        return 0;
    }
}
