package cn.cccs7.cynexus.market.domain.etf;

import cn.cccs7.cynexus.market.domain.entity.MarketEtfBO;
import cn.cccs7.cynexus.market.domain.entity.MarketEtfDataBO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
public class MarketEtfDomainService {

    public MarketEtfBO build(MarketEtfDataBO data, int rowLimit) {
        List<MarketEtfDataBO.Spot> spots = data.getSpots() == null ? List.of() : data.getSpots();
        List<MarketEtfDataBO.Daily> dailies = data.getDailies() == null ? List.of() : data.getDailies();
        if (spots.isEmpty()) {
            return MarketEtfBO.builder().ok(false).error("ETF market snapshot is empty").source(data.getSource()).build();
        }
        Map<String, MarketEtfDataBO.Daily> latestDaily = dailies.stream()
                .sorted(Comparator.comparing(MarketEtfDataBO.Daily::getTradeDate).reversed())
                .collect(Collectors.toMap(MarketEtfDataBO.Daily::getCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<MarketEtfBO.Row> rows = spots.stream().map(item -> row(item, latestDaily.get(item.getCode()))).toList();
        List<MarketEtfBO.Row> broad = rows.stream().filter(item -> hasCategory(spots, item.getCode(), "broad"))
                .sorted(byAmount()).limit(rowLimit).toList();
        List<MarketEtfBO.Row> sectorRows = rows.stream().filter(item -> hasCategory(spots, item.getCode(), "sector"))
                .sorted(byAmount()).limit(rowLimit).toList();
        List<MarketEtfBO.Rotation> rotations = rotations(spots, latestDaily);
        MarketEtfBO.Summary summary = summary(spots, latestDaily);
        return MarketEtfBO.builder()
                .ok(true)
                .tradeDate(data.getTradeDate())
                .fetchedAt(data.getFetchedAt())
                .source(data.getSource())
                .summary(summary)
                .breadth(breadth(data.getBreadth()))
                .history(history(dailies, data.getBreadth()))
                .broad(broad)
                .sectorRows(sectorRows)
                .rotations(rotations)
                .mapping(mapping(data.getMapping()))
                .build();
    }

    private MarketEtfBO.Summary summary(List<MarketEtfDataBO.Spot> spots, Map<String, MarketEtfDataBO.Daily> daily) {
        List<MarketEtfDataBO.Spot> broad = spots.stream().filter(item -> "broad".equals(item.getCategory())).toList();
        List<MarketEtfDataBO.Spot> sector = spots.stream().filter(item -> "sector".equals(item.getCategory())).toList();
        BigDecimal broadShare = sumDelta(broad, daily);
        BigDecimal sectorShare = sumDelta(sector, daily);
        BigDecimal broadProxy = sumProxy(broad, daily);
        BigDecimal sectorProxy = sumProxy(sector, daily);
        int classificationFallbackCount = (int) spots.stream().filter(item -> item.getClassificationSource() == null || item.getClassificationSource().startsWith("fallback.")).count();
        int classificationAuthorityCount = spots.size() - classificationFallbackCount;
        BigDecimal classificationCoveragePct = spots.isEmpty() ? null : BigDecimal.valueOf(classificationAuthorityCount)
                .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(spots.size()), 4, RoundingMode.HALF_UP);
        BigDecimal totalAmount = sum(spots, MarketEtfDataBO.Spot::getAmount);
        BigDecimal activeVolume = sum(spots, MarketEtfDataBO.Spot::getVolume);
        BigDecimal premium = average(spots, MarketEtfDataBO.Spot::getPremiumPct);
        boolean broadInflow = broadProxy != null && broadProxy.signum() > 0;
        boolean sectorInflow = sectorProxy != null && sectorProxy.signum() > 0;
        String direction = broadInflow && sectorInflow ? "risk_on" : !broadInflow && !sectorInflow ? "risk_off" : "rotation";
        String text = switch (direction) {
            case "risk_on" -> "宽基与行业 ETF 同步吸金";
            case "risk_off" -> "宽基与行业 ETF 同步走弱";
            default -> "资金在宽基与行业之间轮动";
        };
        return MarketEtfBO.Summary.builder().totalEtfs(spots.size()).broadCount(broad.size()).sectorCount(sector.size())
                .totalAmount(totalAmount).activeVolume(activeVolume).broadShareDelta(broadShare).sectorShareDelta(sectorShare)
                .broadNetSubscriptionProxy(broadProxy).sectorNetSubscriptionProxy(sectorProxy)
                .averagePremiumPct(premium).classificationAuthorityCount(classificationAuthorityCount)
                .classificationFallbackCount(classificationFallbackCount).classificationCoveragePct(classificationCoveragePct)
                .direction(direction).directionText(text).build();
    }

    private List<MarketEtfBO.Rotation> rotations(List<MarketEtfDataBO.Spot> spots, Map<String, MarketEtfDataBO.Daily> daily) {
        Map<String, List<MarketEtfDataBO.Spot>> grouped = spots.stream().filter(item -> "sector".equals(item.getCategory()))
                .filter(item -> item.getTheme() != null && !item.getTheme().isBlank())
                .collect(Collectors.groupingBy(MarketEtfDataBO.Spot::getTheme, LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream().map(entry -> {
            List<MarketEtfDataBO.Spot> values = entry.getValue();
            MarketEtfDataBO.Spot leader = values.stream().max(byAmountSpot()).orElse(values.getFirst());
            return MarketEtfBO.Rotation.builder().theme(entry.getKey()).etfCount(values.size())
                    .amount(sum(values, MarketEtfDataBO.Spot::getAmount)).shareDelta(sumDelta(values, daily))
                    .netSubscriptionProxy(sumProxy(values, daily)).averageChangePct(average(values, MarketEtfDataBO.Spot::getChangePct))
                    .leaderCode(leader.getCode()).leaderName(leader.getName()).build();
        }).sorted(Comparator.comparing(MarketEtfBO.Rotation::getAmount, Comparator.nullsLast(Comparator.reverseOrder()))).limit(24).toList();
    }

    private List<MarketEtfBO.Trend> history(List<MarketEtfDataBO.Daily> rows, MarketEtfDataBO.Breadth breadth) {
        Map<LocalDate, List<MarketEtfDataBO.Daily>> grouped = rows.stream().collect(Collectors.groupingBy(MarketEtfDataBO.Daily::getTradeDate, LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> {
            List<MarketEtfDataBO.Daily> values = entry.getValue();
            return MarketEtfBO.Trend.builder().tradeDate(entry.getKey())
                    .broadShareDelta(sum(values.stream().filter(item -> "broad".equals(item.getCategory())).toList(), MarketEtfDataBO.Daily::getShareDelta))
                    .sectorShareDelta(sum(values.stream().filter(item -> "sector".equals(item.getCategory())).toList(), MarketEtfDataBO.Daily::getShareDelta))
                    .broadNetSubscriptionProxy(sum(values.stream().filter(item -> "broad".equals(item.getCategory())).toList(), MarketEtfDataBO.Daily::getNetSubscriptionProxy))
                    .sectorNetSubscriptionProxy(sum(values.stream().filter(item -> "sector".equals(item.getCategory())).toList(), MarketEtfDataBO.Daily::getNetSubscriptionProxy))
                    .amount(sum(values, MarketEtfDataBO.Daily::getAmount)).averagePremiumPct(average(values, MarketEtfDataBO.Daily::getPremiumPct))
                    .advancingPct(breadth == null || breadth.getTradeDate() == null || !breadth.getTradeDate().equals(entry.getKey()) ? null : breadthPct(breadth)).build();
        }).toList();
    }

    private MarketEtfBO.Breadth breadth(MarketEtfDataBO.Breadth value) {
        if (value == null) return null;
        return MarketEtfBO.Breadth.builder().tradeDate(value.getTradeDate()).stockCount(value.getStockCount()).risingCount(value.getRisingCount())
                .fallingCount(value.getFallingCount()).flatCount(value.getFlatCount()).limitUpCount(value.getLimitUpCount()).limitDownCount(value.getLimitDownCount())
                .advancingPct(breadthPct(value)).source(value.getSource()).build();
    }

    private MarketEtfBO.MappingSummary mapping(MarketEtfDataBO.MappingSummary value) {
        if (value == null) return null;
        return MarketEtfBO.MappingSummary.builder()
                .catalogEtfCount(value.getCatalogEtfCount())
                .mappedEtfCount(value.getMappedEtfCount())
                .unmappedEtfCount(value.getUnmappedEtfCount())
                .relationshipCount(value.getRelationshipCount())
                .source(value.getSource())
                .dimensions(value.getDimensions() == null ? List.of() : value.getDimensions().stream()
                        .map(item -> MarketEtfBO.MappingDimension.builder().group(item.getGroup()).etfCount(item.getEtfCount())
                                .relationshipCount(item.getRelationshipCount()).build()).toList())
                .tags(value.getTags() == null ? List.of() : value.getTags().stream()
                        .map(item -> MarketEtfBO.MappingTag.builder().group(item.getGroup()).code(item.getCode()).name(item.getName())
                                .etfCount(item.getEtfCount()).relationshipCount(item.getRelationshipCount()).build()).toList())
                .build();
    }

    private MarketEtfBO.Row row(MarketEtfDataBO.Spot spot, MarketEtfDataBO.Daily daily) {
        return MarketEtfBO.Row.builder().code(spot.getCode()).exchange(spot.getExchange()).name(spot.getName()).theme(spot.getTheme())
                .price(spot.getPrice()).changePct(spot.getChangePct()).amount(spot.getAmount()).volume(spot.getVolume()).iopv(spot.getIopv())
                .premiumPct(spot.getPremiumPct()).turnoverPct(spot.getTurnoverPct()).latestShares(spot.getLatestShares())
                .shareDelta(daily == null ? null : daily.getShareDelta()).netSubscriptionProxy(daily == null ? null : daily.getNetSubscriptionProxy())
                .cumulativeShareDelta(daily == null ? null : daily.getCumulativeShareDelta())
                .cumulativeNetSubscriptionProxy(daily == null ? null : daily.getCumulativeNetSubscriptionProxy())
                .insideVolume(spot.getInsideVolume()).outsideVolume(spot.getOutsideVolume())
                .superLargeNetInflow(spot.getSuperLargeNetInflow()).superLargeNetRatio(spot.getSuperLargeNetRatio())
                .mainNetInflow(spot.getMainNetInflow()).mainNetRatio(spot.getMainNetRatio())
                .largeNetInflow(spot.getLargeNetInflow()).largeNetRatio(spot.getLargeNetRatio())
                .mediumNetInflow(spot.getMediumNetInflow()).mediumNetRatio(spot.getMediumNetRatio())
                .smallNetInflow(spot.getSmallNetInflow()).smallNetRatio(spot.getSmallNetRatio()).build();
    }

    private boolean hasCategory(List<MarketEtfDataBO.Spot> spots, String code, String category) {
        return spots.stream().anyMatch(item -> Objects.equals(item.getCode(), code) && category.equals(item.getCategory()));
    }

    private Comparator<MarketEtfBO.Row> byAmount() { return Comparator.comparing(MarketEtfBO.Row::getAmount, Comparator.nullsLast(Comparator.reverseOrder())); }
    private Comparator<MarketEtfDataBO.Spot> byAmountSpot() { return Comparator.comparing(MarketEtfDataBO.Spot::getAmount, Comparator.nullsLast(Comparator.reverseOrder())); }

    private BigDecimal sumDelta(List<MarketEtfDataBO.Spot> spots, Map<String, MarketEtfDataBO.Daily> daily) { return sum(spots.stream().map(item -> daily.get(item.getCode())).filter(Objects::nonNull).toList(), MarketEtfDataBO.Daily::getShareDelta); }
    private BigDecimal sumProxy(List<MarketEtfDataBO.Spot> spots, Map<String, MarketEtfDataBO.Daily> daily) { return sum(spots.stream().map(item -> daily.get(item.getCode())).filter(Objects::nonNull).toList(), MarketEtfDataBO.Daily::getNetSubscriptionProxy); }
    private <T> BigDecimal sum(List<T> rows, java.util.function.Function<T, BigDecimal> getter) { return rows.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private <T> BigDecimal average(List<T> rows, java.util.function.Function<T, BigDecimal> getter) { List<BigDecimal> values = rows.stream().map(getter).filter(Objects::nonNull).toList(); return values.isEmpty() ? null : sum(values, item -> item).divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP); }
    private BigDecimal breadthPct(MarketEtfDataBO.Breadth value) { return value.getStockCount() == null || value.getStockCount() == 0 || value.getRisingCount() == null ? null : BigDecimal.valueOf(value.getRisingCount()).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(value.getStockCount()), 4, RoundingMode.HALF_UP); }
}
