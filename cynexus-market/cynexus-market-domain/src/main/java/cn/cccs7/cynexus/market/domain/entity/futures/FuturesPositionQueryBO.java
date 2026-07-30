package cn.cccs7.cynexus.market.domain.entity.futures;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class FuturesPositionQueryBO {
    private FuturesPositionQueryBO() {
    }

    public record Page(int number, int size, int offset) {
    }

    public record Instrument(String instrumentCode, String varietyCode, String contractCode,
                             BigDecimal volume, BigDecimal longPosition, BigDecimal shortPosition,
                             BigDecimal netPosition, BigDecimal longShortRatio,
                             BigDecimal longChange, BigDecimal shortChange, String source) {
    }

    public record Aggregate(BigDecimal volume, BigDecimal longPosition, BigDecimal shortPosition,
                            BigDecimal netPosition, BigDecimal longShortRatio,
                            BigDecimal longChange, BigDecimal shortChange) {
    }

    public record Member(LocalDate tradeDate, String exchangeCode, String scopeType,
                         String instrumentCode, String varietyCode, String contractCode,
                         String rankType, Integer rankNo, String memberName,
                         BigDecimal rankValue, BigDecimal rankChange, String source) {
    }

    public record History(LocalDate tradeDate, String instrumentCode, String varietyCode,
                          BigDecimal volume, BigDecimal longPosition, BigDecimal shortPosition,
                          BigDecimal netPosition, BigDecimal longShortRatio,
                          BigDecimal longChange, BigDecimal shortChange, String source) {
    }

    public record CoverageItem(LocalDate tradeDate, String exchangeCode, String provider,
                               String status, boolean selected, int expectedInstrumentCount,
                               int actualInstrumentCount, int rowCount, OffsetDateTime completedAt) {
    }

    public record Coverage(LocalDate tradeDate, List<CoverageItem> items) {
    }

    public record Latest(boolean ok, String error, LocalDate requestedTradeDate,
                         LocalDate tradeDate, boolean fallbackTradeDate, String scopeType,
                         String exchangeCode, String varietyCode, int top, Page page, int count,
                         List<Instrument> instruments, Aggregate aggregate, Coverage coverage,
                         List<String> warnings) {
    }
}
