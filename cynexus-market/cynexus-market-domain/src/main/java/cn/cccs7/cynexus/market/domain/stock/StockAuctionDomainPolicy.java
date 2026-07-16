package cn.cccs7.cynexus.market.domain.stock;

import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Domain calculations for opening/closing auction interpretation. */
public final class StockAuctionDomainPolicy {
    public AuctionGroups group(List<StockChartBO.AuctionPoint> rows) {
        List<StockChartBO.AuctionPoint> all = rows == null ? List.of() : rows;
        List<StockChartBO.AuctionPoint> opening = all.stream()
                .filter(row -> "opening".equals(row.getPhase()) || "auction_0925".equals(row.getPhase())).toList();
        List<StockChartBO.AuctionPoint> closing = all.stream()
                .filter(row -> "closing".equals(row.getPhase())).toList();
        StockChartBO.AuctionPoint auction0925 = all.stream()
                .filter(row -> "auction_0925".equals(row.getPhase()) || "09:25:00".equals(row.getTimeLabel()))
                .findFirst().orElse(null);
        return new AuctionGroups(opening, closing, auction0925, all);
    }

    public AuctionPhaseFacts phase(List<StockChartBO.AuctionPoint> rows, String target, String sourcePrefix) {
        if (rows == null || rows.isEmpty()) {
            return new AuctionPhaseFacts(null, null, null, null, null, null, null, false, "低", target,
                    sourcePrefix + "-nearest", null, null, null, null, null, null, null, 0, null, null);
        }
        StockChartBO.AuctionPoint anchor = chooseAnchor(rows, target);
        BigDecimal buy = zero(anchor.getUnmatchedBuyVolume());
        BigDecimal sell = zero(anchor.getUnmatchedSellVolume());
        BigDecimal delta = buy.subtract(sell);
        boolean exact = target.equals(anchor.getTimeLabel());
        return new AuctionPhaseFacts(anchor.getPrice(), anchor.getPrice(), anchor.getMatchedVolume(), anchor.getMatchedAmount(),
                anchor.getUnmatchedBuyVolume(), anchor.getUnmatchedSellVolume(), anchor.getTimeLabel(), exact,
                exact ? "高" : "中", target, sourcePrefix + (exact ? "-exact" : "-nearest"), delta, strengthLabel(delta),
                priceRange(rows), recentPriceTrend(rows), recentPriceChange(rows), recentVolumeDelta(rows),
                directionStability(rows), directionFlipCount(rows), strengthLabel(sumUnmatchedDelta(rows)), imbalancePressure(buy, sell));
    }

    public AuctionPointFacts point(StockChartBO.AuctionPoint value) {
        BigDecimal buy = zero(value.getUnmatchedBuyVolume());
        BigDecimal sell = zero(value.getUnmatchedSellVolume());
        return new AuctionPointFacts(value.getTimeLabel(), value.getPrice(), value.getMatchedVolume(), buy.add(sell),
                directionRaw(buy, sell), value.getMatchedAmount());
    }

    public Map<String, Object> auction0925(StockChartBO.AuctionPoint value) {
        if (value == null) {
            return Map.of("has_auction_0925", false);
        }
        return Map.of("has_auction_0925", true, "price", value.getPrice(), "volume", value.getMatchedVolume(),
                "amount", value.getMatchedAmount());
    }
    public StockChartBO.AuctionPoint chooseAnchor(List<StockChartBO.AuctionPoint> rows, String target) {
        return rows.stream().filter(row -> target.equals(row.getTimeLabel())).findFirst().orElseGet(() -> rows.stream()
                .filter(row -> row.getTimeLabel() != null && row.getTimeLabel().compareTo(target) <= 0)
                .max(Comparator.comparing(StockChartBO.AuctionPoint::getTimeLabel)).orElse(rows.getLast()));
    }
    public PriceRange priceRange(List<StockChartBO.AuctionPoint> rows) {
        List<BigDecimal> prices = rows.stream().map(StockChartBO.AuctionPoint::getPrice).filter(item -> item != null).toList();
        if (prices.isEmpty()) return null;
        BigDecimal low = prices.stream().min(Comparator.naturalOrder()).orElse(null);
        BigDecimal high = prices.stream().max(Comparator.naturalOrder()).orElse(null);
        return new PriceRange(low, high, high.subtract(low));
    }
    public BigDecimal recentPriceChange(List<StockChartBO.AuctionPoint> rows) { return delta(rows, true); }
    public BigDecimal recentVolumeDelta(List<StockChartBO.AuctionPoint> rows) { return delta(rows, false); }
    public String recentPriceTrend(List<StockChartBO.AuctionPoint> rows) {
        BigDecimal change = recentPriceChange(rows); if (change == null) return null;
        return change.signum() > 0 ? "上行" : change.signum() < 0 ? "下行" : "走平";
    }
    public int directionFlipCount(List<StockChartBO.AuctionPoint> rows) {
        int flips = 0, previous = 0;
        for (StockChartBO.AuctionPoint row : rows) { int current = directionRaw(zero(row.getUnmatchedBuyVolume()), zero(row.getUnmatchedSellVolume()));
            if (current != 0) { if (previous != 0 && previous != current) flips++; previous = current; } }
        return flips;
    }
    public String directionStability(List<StockChartBO.AuctionPoint> rows) { int flips = directionFlipCount(rows); return flips == 0 ? "方向稳定" : flips <= 2 ? "方向轻微切换" : "方向频繁切换"; }
    public String strengthLabel(BigDecimal value) { return value == null ? null : value.signum() > 0 ? "买方占优" : value.signum() < 0 ? "卖方占优" : "均衡"; }
    public BigDecimal sumUnmatchedDelta(List<StockChartBO.AuctionPoint> rows) { return rows.stream().map(row -> zero(row.getUnmatchedBuyVolume()).subtract(zero(row.getUnmatchedSellVolume()))).reduce(BigDecimal.ZERO, BigDecimal::add); }
    public BigDecimal imbalancePressure(BigDecimal buy, BigDecimal sell) { BigDecimal total = buy.add(sell); return total.signum() == 0 ? null : buy.subtract(sell).abs().divide(total, 6, RoundingMode.HALF_UP); }
    public BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    public int directionRaw(BigDecimal buy, BigDecimal sell) { return buy.compareTo(sell) > 0 ? 1 : sell.compareTo(buy) > 0 ? -1 : 0; }
    private BigDecimal delta(List<StockChartBO.AuctionPoint> rows, boolean price) {
        List<BigDecimal> values = rows.stream().map(row -> price ? row.getPrice() : row.getMatchedVolume()).filter(item -> item != null).toList();
        return values.size() < 2 ? null : values.getLast().subtract(values.get(values.size() - 2));
    }
    public record PriceRange(BigDecimal low, BigDecimal high, BigDecimal spread) { }
    public record AuctionGroups(List<StockChartBO.AuctionPoint> opening,
                                List<StockChartBO.AuctionPoint> closing,
                                StockChartBO.AuctionPoint auction0925,
                                List<StockChartBO.AuctionPoint> all) { }
    public record AuctionPointFacts(String timeLabel, BigDecimal price, BigDecimal matchedVolume,
                                    BigDecimal unmatchedVolume, int unmatchedDirectionRaw, BigDecimal matchedAmount) { }
    public record AuctionPhaseFacts(BigDecimal price, BigDecimal matchPrice, BigDecimal volume, BigDecimal amount,
                                    BigDecimal unmatchedBuyVolume, BigDecimal unmatchedSellVolume, String time,
                                    boolean anchorExact, String dataConfidence, String anchorTargetTime,
                                    String anchorSource, BigDecimal unmatchedDelta, String strengthLabel,
                                    PriceRange priceRange, String recentPriceTrend, BigDecimal recentPriceChange,
                                    BigDecimal recentVolumeDelta, String directionStability, int directionFlipCount,
                                    String dominantDirection, BigDecimal imbalancePressure) { }
}
