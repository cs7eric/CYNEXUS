package cn.cccs7.cynexus.market.domain.portfolio;
import java.math.BigDecimal; import java.time.OffsetDateTime;
public record PortfolioTradePlanBO(Long planId,String symbol,PortfolioInstrumentType instrumentType,String strategy,String thesis,BigDecimal plannedEntryPrice,BigDecimal stopLossPrice,BigDecimal targetPrice,BigDecimal plannedPositionPct,BigDecimal riskAmount,Integer expectedHoldingDays,BigDecimal confidenceScore,String marketRegime,OffsetDateTime createdAt) {
 public PortfolioTradePlanBO { if(symbol==null||symbol.isBlank()||instrumentType==null||plannedEntryPrice==null||stopLossPrice==null||plannedEntryPrice.signum()<=0||stopLossPrice.signum()<=0) throw new IllegalArgumentException("invalid trade plan"); }
 public BigDecimal plannedRisk(BigDecimal quantity, BigDecimal fee){ return plannedEntryPrice.subtract(stopLossPrice).abs().multiply(quantity==null?BigDecimal.ZERO:quantity).add(fee==null?BigDecimal.ZERO:fee); }
}
