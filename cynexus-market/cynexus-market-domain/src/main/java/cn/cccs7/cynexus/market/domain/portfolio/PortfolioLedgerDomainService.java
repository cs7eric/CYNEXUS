package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PortfolioLedgerDomainService {
    private static final int MONEY_SCALE = 8;
    /** Brokerage screens expose the average cost at three decimal places. */
    private static final int POSITION_PRICE_SCALE = 3;

    public PortfolioLedgerResultBO calculate(List<PortfolioTradeBO> trades, Map<String, BigDecimal> latestPrices) {
        Map<String, PositionAccumulator> positions = new HashMap<>();
        List<PortfolioTradeBO> ordered = trades == null ? List.of() : trades.stream()
                .filter(trade -> trade != null && trade.status() == TradeStatus.ACTIVE)
                .sorted(Comparator.comparing(PortfolioTradeBO::tradedAt).thenComparing(PortfolioTradeBO::id))
                .toList();

        BigDecimal realizedPnl = BigDecimal.ZERO;
        List<PortfolioLedgerWarningBO> warnings = new ArrayList<>();
        for (PortfolioTradeBO trade : ordered) {
            PositionAccumulator position = positions.computeIfAbsent(trade.symbol(),
                    ignored -> new PositionAccumulator(trade.symbol(), trade.instrumentType()));
            if (trade.side() == TradeSide.BUY) {
                position.buy(trade.quantity(), trade.price(), trade.totalFee());
            } else {
                SaleResult sale = position.sell(trade.quantity(), trade.price(), trade.totalFee());
                realizedPnl = realizedPnl.add(sale.realizedPnl());
                if (sale.unmatchedQuantity().signum() > 0) {
                    warnings.add(new PortfolioLedgerWarningBO(
                            "UNMATCHED_SELL",
                            trade.symbol(),
                            money(sale.unmatchedQuantity()),
                            "Sell quantity exceeds imported buy history; unmatched quantity was excluded from P&L."));
                }
            }
        }

        List<PortfolioPositionBO> snapshots = new ArrayList<>();
        BigDecimal unrealizedPnl = BigDecimal.ZERO;
        for (PositionAccumulator position : positions.values()) {
            BigDecimal latestPrice = latestPrices == null ? null : latestPrices.get(position.symbol);
            PortfolioPositionBO snapshot = position.snapshot(latestPrice);
            if (snapshot.quantity().signum() > 0 || snapshot.realizedPnl().signum() != 0) {
                snapshots.add(snapshot);
            }
            unrealizedPnl = unrealizedPnl.add(snapshot.unrealizedPnl());
        }
        snapshots.sort(Comparator.comparing(PortfolioPositionBO::symbol));
        return new PortfolioLedgerResultBO(snapshots, money(realizedPnl), money(unrealizedPnl),
                money(realizedPnl.add(unrealizedPnl)), warnings);
    }

    public PortfolioValuationStatus valuationStatus(List<PortfolioTradeBO> trades,
                                                     PortfolioLedgerResultBO result,
                                                     Map<String, BigDecimal> latestPrices) {
        boolean hasActiveTrades = trades != null && trades.stream()
                .anyMatch(trade -> trade != null && trade.status() == TradeStatus.ACTIVE);
        if (!hasActiveTrades) {
            return PortfolioValuationStatus.NO_CONFIRMED_TRADES;
        }

        long openPositions = result == null ? 0 : result.positions().stream()
                .filter(position -> position.quantity() != null && position.quantity().signum() > 0)
                .count();
        if (openPositions == 0) {
            return PortfolioValuationStatus.CURRENT_MARKET;
        }

        long pricedPositions = result.positions().stream()
                .filter(position -> position.quantity() != null && position.quantity().signum() > 0)
                .filter(position -> latestPrices != null
                        && latestPrices.get(position.symbol()) != null)
                .count();
        if (pricedPositions == 0) {
            return PortfolioValuationStatus.LATEST_PRICE_UNAVAILABLE;
        }
        return pricedPositions == openPositions
                ? PortfolioValuationStatus.CURRENT_MARKET
                : PortfolioValuationStatus.PARTIAL_MARKET_DATA;
    }

    public PortfolioLedgerResultBO applyRealizedPnlBaselines(PortfolioLedgerResultBO result,
                                                               Map<String, BigDecimal> baselines) {
        if (result == null || baselines == null || baselines.isEmpty()) {
            return result;
        }
        List<PortfolioPositionBO> positions = result.positions().stream()
                .map(position -> {
                    BigDecimal baseline = baselines.getOrDefault(position.symbol(), BigDecimal.ZERO);
                    BigDecimal realized = money(position.realizedPnl().subtract(baseline));
                    return new PortfolioPositionBO(position.symbol(), position.instrumentType(), position.quantity(),
                            position.averageCost(), position.costBasis(), realized, position.latestPrice(),
                            position.marketValue(), position.unrealizedPnl(), position.unrealizedReturnRate());
                })
                .filter(position -> position.quantity().signum() > 0 || position.realizedPnl().signum() != 0)
                .toList();
        BigDecimal realizedPnl = positions.stream().map(PortfolioPositionBO::realizedPnl)
                .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PortfolioLedgerResultBO(positions, money(realizedPnl), result.unrealizedPnl(),
                money(realizedPnl.add(result.unrealizedPnl())), result.warnings());
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static final class PositionAccumulator {
        private final String symbol;
        private final PortfolioInstrumentType instrumentType;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal costBasis = BigDecimal.ZERO;
        private BigDecimal realizedPnl = BigDecimal.ZERO;

        private PositionAccumulator(String symbol, PortfolioInstrumentType instrumentType) {
            this.symbol = symbol;
            this.instrumentType = instrumentType;
        }

        private void buy(BigDecimal quantity, BigDecimal price, BigDecimal fee) {
            this.quantity = this.quantity.add(quantity);
            this.costBasis = this.costBasis.add(quantity.multiply(price).add(fee));
        }

        private SaleResult sell(BigDecimal quantity, BigDecimal price, BigDecimal fee) {
            BigDecimal matchedQuantity = quantity.min(this.quantity);
            BigDecimal unmatchedQuantity = quantity.subtract(matchedQuantity);
            if (matchedQuantity.signum() == 0) {
                return new SaleResult(BigDecimal.ZERO, unmatchedQuantity);
            }
            BigDecimal averageCost = averageCost();
            BigDecimal matchedFee = quantity.signum() == 0
                    ? BigDecimal.ZERO
                    : fee.multiply(matchedQuantity).divide(quantity, MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal saleNet = matchedQuantity.multiply(price).subtract(matchedFee);
            BigDecimal releasedCost = matchedQuantity.multiply(averageCost);
            BigDecimal realized = saleNet.subtract(releasedCost);
            this.quantity = this.quantity.subtract(matchedQuantity);
            this.costBasis = this.costBasis.subtract(releasedCost);
            this.realizedPnl = this.realizedPnl.add(realized);
            if (this.quantity.signum() == 0) {
                this.costBasis = BigDecimal.ZERO;
            }
            return new SaleResult(realized, unmatchedQuantity);
        }

        private BigDecimal averageCost() {
            return quantity.signum() == 0 ? BigDecimal.ZERO : costBasis.divide(quantity, MONEY_SCALE, RoundingMode.HALF_UP);
        }

        private PortfolioPositionBO snapshot(BigDecimal latestPrice) {
            BigDecimal averageCost = averageCost().setScale(POSITION_PRICE_SCALE, RoundingMode.HALF_UP);
            // Keep the ledger's exact cost basis for P&L. The displayed average
            // cost is intentionally rounded, but using it here distorts today's
            // P&L when a position changes during the current trading day.
            BigDecimal valuationCostBasis = money(costBasis);
            BigDecimal marketValue = latestPrice == null ? BigDecimal.ZERO : quantity.multiply(latestPrice);
            BigDecimal unrealized = latestPrice == null ? BigDecimal.ZERO : marketValue.subtract(valuationCostBasis);
            BigDecimal returnRate = valuationCostBasis.signum() == 0 ? BigDecimal.ZERO
                    : unrealized.divide(valuationCostBasis, MONEY_SCALE, RoundingMode.HALF_UP);
            return new PortfolioPositionBO(symbol, instrumentType, quantity, money(averageCost), money(valuationCostBasis), money(realizedPnl),
                    latestPrice, money(marketValue), money(unrealized), returnRate);
        }
    }

    private record SaleResult(BigDecimal realizedPnl, BigDecimal unmatchedQuantity) {
    }
}
