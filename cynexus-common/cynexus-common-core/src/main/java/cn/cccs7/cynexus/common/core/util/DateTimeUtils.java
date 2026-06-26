package cn.cccs7.cynexus.common.core.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class DateTimeUtils {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    private DateTimeUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }
}
