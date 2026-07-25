package cn.cccs7.cynexus.common.core.util;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class DateTimeUtils {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID);

    private DateTimeUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }
}
