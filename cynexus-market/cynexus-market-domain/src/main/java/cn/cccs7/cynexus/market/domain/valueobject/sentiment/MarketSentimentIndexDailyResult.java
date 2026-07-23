package cn.cccs7.cynexus.market.domain.valueobject.sentiment;

import cn.cccs7.cynexus.market.domain.entity.IndexDailyBarBO;
import java.time.LocalDate;
import java.util.List;

public record MarketSentimentIndexDailyResult(
        String code,
        String name,
        LocalDate start,
        LocalDate end,
        List<IndexDailyBarBO> items) {

    public MarketSentimentIndexDailyResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
