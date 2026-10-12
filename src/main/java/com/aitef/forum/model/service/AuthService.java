package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.UserDao;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.util.PasswordUtil;
import com.aitef.forum.util.ValidationException;

import java.util.Optional;
import java.util.regex.Pattern;

public class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,40}$");
    private final UserDao userDao = new UserDao();

    public User login(String account, String password) {//登录校正
        if (isBlank(account) || isBlank(password)) {
            throw new ValidationException("请输入账号和密码");
        }
        Optional<User> userOptional = userDao.findByUsernameOrEmail(account.trim());
        if (userOptional.isEmpty() || !PasswordUtil.verify(password, userOptional.get().getPasswordHash())) {
            throw new ValidationException("账号或密码不正确");
        }
        User user = userOptional.get();
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ValidationException("账号当前不可用");
        }
        userDao.updateLastSeen(user.getId());
        return userDao.findById(user.getId()).orElse(user);
    }

    public User register(String username, String email, String password, String displayName) {
        username = username == null ? "" : username.trim();
        email = email == null ? "" : email.trim();
        displayName = displayName == null || displayName.isBlank() ? username : displayName.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException("用户名只能包含字母、数字和下划线，长度 3-40 位");
        }
        if (!email.contains("@") || email.length() > 120) {
            throw new ValidationException("请输入有效邮箱");
        }
        if (password == null || password.length() < 8) {
            throw new ValidationException("密码至少需要 8 位");
        }
        if (userDao.existsByUsernameOrEmail(username, email)) {
            throw new ValidationException("用户名或邮箱已被使用");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hash(password)); //注册调用
        user.setDisplayName(displayName);
        user.setAvatarUrl("https://api.dicebear.com/9.x/initials/svg?seed=" + username);
        user.setBio("");
        user.setLocation("");
        user.setWebsite("");
        user.setTrustLevel(0);
        user.setRole("USER");
        Long userId = userDao.create(user);
        return userDao.findById(userId).orElseThrow();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
