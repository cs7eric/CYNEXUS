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

    public PortfolioLedgerResultBO calculate(List<PortfolioTradeBO> trades, Map<String, BigDecimal> latestPrices) {
        Map<String, PositionAccumulator> positions = new HashMap<>();
        List<PortfolioTradeBO> ordered = trades == null ? List.of() : trades.stream()
                .filter(trade -> trade != null && trade.status() == TradeStatus.ACTIVE)
                .sorted(Comparator.comparing(PortfolioTradeBO::tradedAt).thenComparing(PortfolioTradeBO::id))
                .toList();

        BigDecimal realizedPnl = BigDecimal.ZERO;
        for (PortfolioTradeBO trade : ordered) {
            PositionAccumulator position = positions.computeIfAbsent(trade.symbol(), PositionAccumulator::new);
            if (trade.side() == TradeSide.BUY) {
                position.buy(trade.quantity(), trade.price(), trade.totalFee());
            } else {
                realizedPnl = realizedPnl.add(position.sell(trade.quantity(), trade.price(), trade.totalFee()));
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
                money(realizedPnl.add(unrealizedPnl)));
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

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static final class PositionAccumulator {
        private final String symbol;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal costBasis = BigDecimal.ZERO;
        private BigDecimal realizedPnl = BigDecimal.ZERO;

        private PositionAccumulator(String symbol) {
            this.symbol = symbol;
        }

        private void buy(BigDecimal quantity, BigDecimal price, BigDecimal fee) {
            this.quantity = this.quantity.add(quantity);
            this.costBasis = this.costBasis.add(quantity.multiply(price).add(fee));
        }

        private BigDecimal sell(BigDecimal quantity, BigDecimal price, BigDecimal fee) {
            if (quantity.compareTo(this.quantity) > 0) {
                throw new IllegalStateException("sell quantity exceeds current position for " + symbol);
            }
            BigDecimal averageCost = averageCost();
            BigDecimal saleNet = quantity.multiply(price).subtract(fee);
            BigDecimal releasedCost = quantity.multiply(averageCost);
            BigDecimal realized = saleNet.subtract(releasedCost);
            this.quantity = this.quantity.subtract(quantity);
            this.costBasis = this.costBasis.subtract(releasedCost);
            this.realizedPnl = this.realizedPnl.add(realized);
            if (this.quantity.signum() == 0) {
                this.costBasis = BigDecimal.ZERO;
            }
            return realized;
        }

        private BigDecimal averageCost() {
            return quantity.signum() == 0 ? BigDecimal.ZERO : costBasis.divide(quantity, MONEY_SCALE, RoundingMode.HALF_UP);
        }

        private PortfolioPositionBO snapshot(BigDecimal latestPrice) {
            BigDecimal averageCost = averageCost();
            BigDecimal marketValue = latestPrice == null ? BigDecimal.ZERO : quantity.multiply(latestPrice);
            BigDecimal unrealized = latestPrice == null ? BigDecimal.ZERO : marketValue.subtract(costBasis);
            BigDecimal returnRate = costBasis.signum() == 0 ? BigDecimal.ZERO
                    : unrealized.divide(costBasis, MONEY_SCALE, RoundingMode.HALF_UP);
            return new PortfolioPositionBO(symbol, quantity, money(averageCost), money(costBasis), money(realizedPnl),
                    latestPrice, money(marketValue), money(unrealized), returnRate);
        }
    }
}
