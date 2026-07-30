package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class FuturesPositionDTO {
    private FuturesPositionDTO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Latest {
        private boolean ok;
        private String error;
        private LocalDate requestedTradeDate;
        private LocalDate tradeDate;
        private boolean fallbackTradeDate;
        private String scopeType;
        private String exchangeCode;
        private String varietyCode;
        private int top;
        private int page;
        private int pageSize;
        private int count;
        private List<Instrument> instruments;
        private Aggregate aggregate;
        private Coverage coverage;
        private List<String> warnings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Instrument {
        private String exchangeCode;
        private String instrumentCode;
        private String varietyCode;
        private String contractCode;
        private BigDecimal volume;
        private BigDecimal longPosition;
        private BigDecimal shortPosition;
        private BigDecimal netPosition;
        private BigDecimal longShortRatio;
        private BigDecimal longChange;
        private BigDecimal shortChange;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Aggregate {
        private BigDecimal volume;
        private BigDecimal longPosition;
        private BigDecimal shortPosition;
        private BigDecimal netPosition;
        private BigDecimal longShortRatio;
        private BigDecimal longChange;
        private BigDecimal shortChange;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Member {
        private LocalDate tradeDate;
        private String exchangeCode;
        private String scopeType;
        private String instrumentCode;
        private String varietyCode;
        private String contractCode;
        private String rankType;
        private Integer rankNo;
        private String memberName;
        private BigDecimal rankValue;
        private BigDecimal rankChange;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class History {
        private LocalDate tradeDate;
        private String instrumentCode;
        private String varietyCode;
        private BigDecimal volume;
        private BigDecimal longPosition;
        private BigDecimal shortPosition;
        private BigDecimal netPosition;
        private BigDecimal longShortRatio;
        private BigDecimal longChange;
        private BigDecimal shortChange;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coverage {
        private LocalDate tradeDate;
        private List<CoverageItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoverageItem {
        private LocalDate tradeDate;
        private String exchangeCode;
        private String provider;
        private String status;
        private boolean selected;
        private int expectedInstrumentCount;
        private int actualInstrumentCount;
        private int rowCount;
        private OffsetDateTime completedAt;
    }
}
