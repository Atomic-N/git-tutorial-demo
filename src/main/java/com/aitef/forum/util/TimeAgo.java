package com.aitef.forum.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeAgo {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TimeAgo() {
    }

    public static String format(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        Duration duration = Duration.between(time, LocalDateTime.now());
        long seconds = Math.max(0, duration.toSeconds());

        // 刚刚 (0-60秒)
        if (seconds < 60) {
            return "刚刚";
        }

        // X分钟前 (1-59分钟)
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + "分钟前";
        }

        // X小时前 (1-23小时)
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + "小时前";
        }

        // X天前 (1-6天)
        long days = duration.toDays();
        if (days < 7) {
            return days + "天前";
        }

        // 超过7天但在今年：显示 MM-dd
        if (time.getYear() == LocalDateTime.now().getYear()) {
            return time.format(DATE_FORMATTER);
        }

        // 跨年：显示完整日期 yyyy-MM-dd
        return time.format(YEAR_FORMATTER);
    }
}
