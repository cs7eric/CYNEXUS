package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarketEtfBO {

    Boolean ok;
    String error;
    LocalDate tradeDate;
    OffsetDateTime fetchedAt;
    String source;
    Summary summary;
    Breadth breadth;
    List<Trend> history;
    List<Row> broad;
    List<Row> sectorRows;
    List<Rotation> rotations;
    MappingSummary mapping;

    @Value
    @Builder
    public static class Summary {
        Integer totalEtfs;
        Integer broadCount;
        Integer sectorCount;
        BigDecimal totalAmount;
        BigDecimal activeVolume;
        BigDecimal broadShareDelta;
        BigDecimal sectorShareDelta;
        BigDecimal broadNetSubscriptionProxy;
        BigDecimal sectorNetSubscriptionProxy;
        BigDecimal averagePremiumPct;
        Integer classificationAuthorityCount;
        Integer classificationFallbackCount;
        BigDecimal classificationCoveragePct;
        String direction;
        String directionText;
    }

    @Value
    @Builder
    public static class Breadth {
        LocalDate tradeDate;
        Integer stockCount;
        Integer risingCount;
        Integer fallingCount;
        Integer flatCount;
        Integer limitUpCount;
        Integer limitDownCount;
        BigDecimal advancingPct;
        String source;
    }

    @Value
    @Builder
    public static class Trend {
        LocalDate tradeDate;
        BigDecimal broadShareDelta;
        BigDecimal sectorShareDelta;
        BigDecimal broadNetSubscriptionProxy;
        BigDecimal sectorNetSubscriptionProxy;
        BigDecimal amount;
        BigDecimal averagePremiumPct;
        BigDecimal advancingPct;
    }

    @Value
    @Builder
    public static class Row {
        String code;
        String exchange;
        String name;
        String theme;
        BigDecimal price;
        BigDecimal changePct;
        BigDecimal amount;
        BigDecimal volume;
        BigDecimal iopv;
        BigDecimal premiumPct;
        BigDecimal turnoverPct;
        BigDecimal latestShares;
        BigDecimal shareDelta;
        BigDecimal netSubscriptionProxy;
        BigDecimal cumulativeShareDelta;
        BigDecimal cumulativeNetSubscriptionProxy;
        BigDecimal insideVolume;
        BigDecimal outsideVolume;
        BigDecimal superLargeNetInflow;
        BigDecimal superLargeNetRatio;
        BigDecimal mainNetInflow;
        BigDecimal mainNetRatio;
        BigDecimal largeNetInflow;
        BigDecimal largeNetRatio;
        BigDecimal mediumNetInflow;
        BigDecimal mediumNetRatio;
        BigDecimal smallNetInflow;
        BigDecimal smallNetRatio;
    }

    @Value
    @Builder
    public static class Rotation {
        String theme;
        Integer etfCount;
        BigDecimal amount;
        BigDecimal shareDelta;
        BigDecimal netSubscriptionProxy;
        BigDecimal averageChangePct;
        String leaderCode;
        String leaderName;
    }

    @Value
    @Builder
    public static class MappingSummary {
        Integer catalogEtfCount;
        Integer mappedEtfCount;
        Integer unmappedEtfCount;
        Integer relationshipCount;
        String source;
        List<MappingDimension> dimensions;
        List<MappingTag> tags;
    }

    @Value
    @Builder
    public static class MappingDimension {
        String group;
        Integer etfCount;
        Integer relationshipCount;
    }

    @Value
    @Builder
    public static class MappingTag {
        String group;
        String code;
        String name;
        Integer etfCount;
        Integer relationshipCount;
    }
}
