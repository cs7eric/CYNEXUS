package cn.cccs7.cynexus.market.domain.stock.impl;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.market.domain.stock.StockChartDomainService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

@Service
public class StockChartDomainServiceImpl implements StockChartDomainService {

    private static final int DEFAULT_KLINE_COUNT = 520;
    private static final ZoneId SHANGHAI = ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID);

    @Override
    public int defaultKlineCount() {
        return DEFAULT_KLINE_COUNT;
    }

    @Override
    public String normalizePeriod(String period) {
        String value = period == null || period.isBlank() ? "1d" : period.trim().toLowerCase();
        return switch (value) {
            case "1", "1min", "1m" -> "1m";
            case "5", "5min", "5m" -> "5m";
            case "15", "15min", "15m" -> "15m";
            case "30", "30min", "30m" -> "30m";
            case "60", "60min", "60m" -> "60m";
            case "120", "120min", "120m" -> "120m";
            case "day", "daily", "1d" -> "1d";
            default -> "1d";
        };
    }

    @Override
    public int periodMinutes(String period) {
        return switch (normalizePeriod(period)) {
            case "1m" -> 1;
            case "5m" -> 5;
            case "15m" -> 15;
            case "30m" -> 30;
            case "60m" -> 60;
            case "120m" -> 120;
            default -> 1;
        };
    }

    @Override
    public String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    @Override
    public LocalDate parseDateOrToday(String value) {
        return value == null || value.isBlank() ? LocalDate.now(SHANGHAI) : LocalDate.parse(value);
    }

    @Override
    public int auctionHistoryLimit(int requested) {
        return Math.max(1, Math.min(requested, 30));
    }
}
