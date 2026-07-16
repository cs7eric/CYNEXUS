package cn.cccs7.cynexus.market.domain.industry;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class IndustryApplicationDomainPolicy {

    public static final int REFRESH_KLINE_COUNT = 240;
    private static final int DEFAULT_HISTORY_DAYS = 365;
    private static final int MAX_HISTORY_DAYS = 500;
    private static final int MAX_TOP = 500;

    public LocalDate parseTradeDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("trade_date must be yyyy-MM-dd", ex);
        }
    }

    public int normalizeHistoryDays(int value) {
        return Math.max(1, Math.min(value <= 0 ? DEFAULT_HISTORY_DAYS : value, MAX_HISTORY_DAYS));
    }

    public Integer normalizeTop(Integer value) {
        return value == null || value <= 0 ? null : Math.min(value, MAX_TOP);
    }

    public List<IndustryApplicationBO.Target> resolveRefreshTargets(String targetId,
                                                                      IndustryApplicationBO.Config config,
                                                                      List<IndustryApplicationBO.Target> matched) {
        if (targetId != null && !targetId.isBlank()) {
            return matched == null ? List.of() : matched;
        }
        return config == null || config.getItems() == null ? List.of() : config.getItems().stream()
                .filter(item -> item.getEnabled() == null || item.getEnabled())
                .toList();
    }

    public RefreshDecision refreshDecision(IndustryApplicationBO.Target target,
                                            IndustryApplicationBO.Kline kline,
                                            Exception failure) {
        if (target == null) {
            return new RefreshDecision(null, false, 0, "target not found");
        }
        if (failure != null) {
            return new RefreshDecision(target.getTargetKey(), false,
                    kline == null || kline.getBars() == null ? 0 : kline.getBars().size(), failure.getMessage());
        }
        return new RefreshDecision(target.getTargetKey(), true,
                kline == null || kline.getBars() == null ? 0 : kline.getBars().size(), null);
    }

    public record RefreshDecision(String id, boolean ok, int klineCount, String error) {
    }
}
