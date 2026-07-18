package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.MarketEtfDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketEtfBO;
import cn.cccs7.cynexus.market.domain.entity.MarketEtfDataBO;
import cn.cccs7.cynexus.market.domain.etf.MarketEtfDomainService;
import cn.cccs7.cynexus.market.domain.repository.MarketEtfQueryRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketEtfQueryAppService {

    private final MarketEtfQueryRepository repository;
    private final MarketEtfDomainService domainService;

    public MarketEtfQueryAppService(MarketEtfQueryRepository repository, MarketEtfDomainService domainService) {
        this.repository = repository;
        this.domainService = domainService;
    }

    public MarketEtfDTO getMarket(int historyDays, int rowLimit) {
        int boundedRowLimit = Math.max(20, Math.min(rowLimit, 300));
        MarketEtfBO value = domainService.build(repository.load(Math.max(3, Math.min(historyDays, 30)), boundedRowLimit), boundedRowLimit);
        return toDto(value);
    }

    private MarketEtfDTO toDto(MarketEtfBO value) {
        return MarketEtfDTO.builder().ok(value.getOk()).error(value.getError()).tradeDate(value.getTradeDate()).fetchedAt(value.getFetchedAt())
                .source(value.getSource()).summary(summary(value.getSummary())).breadth(breadth(value.getBreadth()))
                .history(value.getHistory() == null ? List.of() : value.getHistory().stream().map(this::trend).toList())
                .broad(value.getBroad() == null ? List.of() : value.getBroad().stream().map(this::row).toList())
                .sectorRows(value.getSectorRows() == null ? List.of() : value.getSectorRows().stream().map(this::row).toList())
                .rotations(value.getRotations() == null ? List.of() : value.getRotations().stream().map(this::rotation).toList())
                .mapping(mapping(value.getMapping())).build();
    }

    private MarketEtfDTO.Summary summary(MarketEtfBO.Summary value) {
        if (value == null) return null;
        return MarketEtfDTO.Summary.builder().totalEtfs(value.getTotalEtfs()).broadCount(value.getBroadCount()).sectorCount(value.getSectorCount())
                .totalAmount(value.getTotalAmount()).activeVolume(value.getActiveVolume()).broadShareDelta(value.getBroadShareDelta())
                .sectorShareDelta(value.getSectorShareDelta()).broadNetSubscriptionProxy(value.getBroadNetSubscriptionProxy())
                .sectorNetSubscriptionProxy(value.getSectorNetSubscriptionProxy()).averagePremiumPct(value.getAveragePremiumPct())
                .classificationAuthorityCount(value.getClassificationAuthorityCount()).classificationFallbackCount(value.getClassificationFallbackCount())
                .classificationCoveragePct(value.getClassificationCoveragePct())
                .direction(value.getDirection()).directionText(value.getDirectionText()).build();
    }

    private MarketEtfDTO.Breadth breadth(MarketEtfBO.Breadth value) {
        if (value == null) return null;
        return MarketEtfDTO.Breadth.builder().tradeDate(value.getTradeDate()).stockCount(value.getStockCount()).risingCount(value.getRisingCount())
                .fallingCount(value.getFallingCount()).flatCount(value.getFlatCount()).limitUpCount(value.getLimitUpCount()).limitDownCount(value.getLimitDownCount())
                .advancingPct(value.getAdvancingPct()).source(value.getSource()).build();
    }

    private MarketEtfDTO.Trend trend(MarketEtfBO.Trend value) {
        return MarketEtfDTO.Trend.builder().tradeDate(value.getTradeDate()).broadShareDelta(value.getBroadShareDelta()).sectorShareDelta(value.getSectorShareDelta())
                .broadNetSubscriptionProxy(value.getBroadNetSubscriptionProxy()).sectorNetSubscriptionProxy(value.getSectorNetSubscriptionProxy())
                .amount(value.getAmount()).averagePremiumPct(value.getAveragePremiumPct()).advancingPct(value.getAdvancingPct()).build();
    }

    private MarketEtfDTO.Row row(MarketEtfBO.Row value) {
        return MarketEtfDTO.Row.builder().code(value.getCode()).exchange(value.getExchange()).name(value.getName()).theme(value.getTheme()).price(value.getPrice())
                .changePct(value.getChangePct()).amount(value.getAmount()).volume(value.getVolume()).iopv(value.getIopv()).premiumPct(value.getPremiumPct())
                .turnoverPct(value.getTurnoverPct()).latestShares(value.getLatestShares()).shareDelta(value.getShareDelta())
                .netSubscriptionProxy(value.getNetSubscriptionProxy()).cumulativeShareDelta(value.getCumulativeShareDelta())
                .cumulativeNetSubscriptionProxy(value.getCumulativeNetSubscriptionProxy()).insideVolume(value.getInsideVolume())
                .outsideVolume(value.getOutsideVolume()).superLargeNetInflow(value.getSuperLargeNetInflow())
                .superLargeNetRatio(value.getSuperLargeNetRatio()).mainNetInflow(value.getMainNetInflow())
                .mainNetRatio(value.getMainNetRatio()).largeNetInflow(value.getLargeNetInflow()).largeNetRatio(value.getLargeNetRatio())
                .mediumNetInflow(value.getMediumNetInflow()).mediumNetRatio(value.getMediumNetRatio())
                .smallNetInflow(value.getSmallNetInflow()).smallNetRatio(value.getSmallNetRatio()).build();
    }

    private MarketEtfDTO.Rotation rotation(MarketEtfBO.Rotation value) {
        return MarketEtfDTO.Rotation.builder().theme(value.getTheme()).etfCount(value.getEtfCount()).amount(value.getAmount()).shareDelta(value.getShareDelta())
                .netSubscriptionProxy(value.getNetSubscriptionProxy()).averageChangePct(value.getAverageChangePct()).leaderCode(value.getLeaderCode()).leaderName(value.getLeaderName()).build();
    }

    private MarketEtfDTO.MappingSummary mapping(MarketEtfBO.MappingSummary value) {
        if (value == null) return null;
        return MarketEtfDTO.MappingSummary.builder()
                .catalogEtfCount(value.getCatalogEtfCount()).mappedEtfCount(value.getMappedEtfCount())
                .unmappedEtfCount(value.getUnmappedEtfCount()).relationshipCount(value.getRelationshipCount())
                .source(value.getSource())
                .dimensions(value.getDimensions() == null ? List.of() : value.getDimensions().stream()
                        .map(item -> MarketEtfDTO.MappingDimension.builder().group(item.getGroup()).etfCount(item.getEtfCount())
                                .relationshipCount(item.getRelationshipCount()).build()).toList())
                .tags(value.getTags() == null ? List.of() : value.getTags().stream()
                        .map(item -> MarketEtfDTO.MappingTag.builder().group(item.getGroup()).code(item.getCode()).name(item.getName())
                                .etfCount(item.getEtfCount()).relationshipCount(item.getRelationshipCount()).build()).toList())
                .build();
    }
}
