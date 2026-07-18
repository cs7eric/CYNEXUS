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
public class MarketEtfDTO {

    private Boolean ok;
    private String error;
    private LocalDate tradeDate;
    private OffsetDateTime fetchedAt;
    private String source;
    private Summary summary;
    private Breadth breadth;
    private List<Trend> history;
    private List<Row> broad;
    private List<Row> sectorRows;
    private List<Rotation> rotations;
    private MappingSummary mapping;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer totalEtfs;
        private Integer broadCount;
        private Integer sectorCount;
        private BigDecimal totalAmount;
        private BigDecimal activeVolume;
        private BigDecimal broadShareDelta;
        private BigDecimal sectorShareDelta;
        private BigDecimal broadNetSubscriptionProxy;
        private BigDecimal sectorNetSubscriptionProxy;
        private BigDecimal averagePremiumPct;
        private Integer classificationAuthorityCount;
        private Integer classificationFallbackCount;
        private BigDecimal classificationCoveragePct;
        private String direction;
        private String directionText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Breadth {
        private LocalDate tradeDate;
        private Integer stockCount;
        private Integer risingCount;
        private Integer fallingCount;
        private Integer flatCount;
        private Integer limitUpCount;
        private Integer limitDownCount;
        private BigDecimal advancingPct;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trend {
        private LocalDate tradeDate;
        private BigDecimal broadShareDelta;
        private BigDecimal sectorShareDelta;
        private BigDecimal broadNetSubscriptionProxy;
        private BigDecimal sectorNetSubscriptionProxy;
        private BigDecimal amount;
        private BigDecimal averagePremiumPct;
        private BigDecimal advancingPct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private String code;
        private String exchange;
        private String name;
        private String theme;
        private BigDecimal price;
        private BigDecimal changePct;
        private BigDecimal amount;
        private BigDecimal volume;
        private BigDecimal iopv;
        private BigDecimal premiumPct;
        private BigDecimal turnoverPct;
        private BigDecimal latestShares;
        private BigDecimal shareDelta;
        private BigDecimal netSubscriptionProxy;
        private BigDecimal cumulativeShareDelta;
        private BigDecimal cumulativeNetSubscriptionProxy;
        private BigDecimal insideVolume;
        private BigDecimal outsideVolume;
        private BigDecimal superLargeNetInflow;
        private BigDecimal superLargeNetRatio;
        private BigDecimal mainNetInflow;
        private BigDecimal mainNetRatio;
        private BigDecimal largeNetInflow;
        private BigDecimal largeNetRatio;
        private BigDecimal mediumNetInflow;
        private BigDecimal mediumNetRatio;
        private BigDecimal smallNetInflow;
        private BigDecimal smallNetRatio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rotation {
        private String theme;
        private Integer etfCount;
        private BigDecimal amount;
        private BigDecimal shareDelta;
        private BigDecimal netSubscriptionProxy;
        private BigDecimal averageChangePct;
        private String leaderCode;
        private String leaderName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MappingSummary {
        private Integer catalogEtfCount;
        private Integer mappedEtfCount;
        private Integer unmappedEtfCount;
        private Integer relationshipCount;
        private String source;
        private List<MappingDimension> dimensions;
        private List<MappingTag> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MappingDimension {
        private String group;
        private Integer etfCount;
        private Integer relationshipCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MappingTag {
        private String group;
        private String code;
        private String name;
        private Integer etfCount;
        private Integer relationshipCount;
    }
}
