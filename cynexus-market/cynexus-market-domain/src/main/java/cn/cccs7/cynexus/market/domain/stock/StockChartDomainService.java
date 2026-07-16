package cn.cccs7.cynexus.market.domain.stock;

import java.time.LocalDate;

public interface StockChartDomainService {

    int defaultKlineCount();

    String normalizePeriod(String period);

    int periodMinutes(String period);

    String defaultValue(String value, String fallback);

    LocalDate parseDateOrToday(String value);

    int auctionHistoryLimit(int requested);
}
