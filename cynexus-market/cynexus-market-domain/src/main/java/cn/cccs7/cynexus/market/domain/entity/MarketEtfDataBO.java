package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarketEtfDataBO {

    LocalDate tradeDate;
    OffsetDateTime fetchedAt;
    String source;
    List<Spot> spots;
    List<Daily> dailies;
    Breadth breadth;
    MappingSummary mapping;

    @Value
    @Builder
    public static class Spot {
        String code;
        String exchange;
        String name;
        String category;
        String theme;
        String classificationSource;
        BigDecimal price;
        BigDecimal prevClose;
        BigDecimal changePct;
        BigDecimal volume;
        BigDecimal amount;
        BigDecimal iopv;
        BigDecimal premiumPct;
        BigDecimal turnoverPct;
        BigDecimal insideVolume;
        BigDecimal outsideVolume;
        BigDecimal latestShares;
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
        OffsetDateTime capturedAt;
    }

    @Value
    @Builder
    public static class Daily {
        LocalDate tradeDate;
        String code;
        String exchange;
        String name;
        String category;
        String theme;
        BigDecimal closePrice;
        BigDecimal iopv;
        BigDecimal premiumPct;
        BigDecimal volume;
        BigDecimal amount;
        BigDecimal totalShares;
        BigDecimal previousShares;
        BigDecimal shareDelta;
        BigDecimal netSubscriptionProxy;
        BigDecimal cumulativeShareDelta;
        BigDecimal cumulativeNetSubscriptionProxy;
        BigDecimal turnoverPct;
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
    public static class Breadth {
        LocalDate tradeDate;
        Integer stockCount;
        Integer risingCount;
        Integer fallingCount;
        Integer flatCount;
        Integer limitUpCount;
        Integer limitDownCount;
        String source;
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
