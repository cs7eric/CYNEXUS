package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketEventReplayDTO {
    private String code;
    private LocalDate tradeDate;
    private OffsetDateTime anchorAt;
    private String status;
    private String source;
    private OffsetDateTime dataCutoffAt;
    private Coverage coverage;
    private List<String> warnings;
    private List<Bar> stockMinuteBars;
    private MarketReplay marketReplay;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Bar {
        private Long timestamp;
        private LocalDate tradeDate;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal volume;
        private BigDecimal turnover;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MarketReplay {
        private String dataStatus;
        private LocalDate dataTradeDate;
        private String source;
        private OffsetDateTime requestedFrom;
        private OffsetDateTime requestedTo;
        private Integer eventCount;
        private Integer industryPointCount;
        private String message;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Coverage {
        private Integer stockBarsRead;
        private Integer stockBarsIncluded;
        private Boolean marketAvailable;
        private Boolean industryAvailable;
        private Boolean stockAvailable;
    }
}
