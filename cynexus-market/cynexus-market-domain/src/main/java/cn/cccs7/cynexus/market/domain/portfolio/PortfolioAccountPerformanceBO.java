package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioAccountPerformanceBO(BigDecimal timeWeightedReturn, BigDecimal moneyWeightedReturn,
                                            BigDecimal maxDrawdown, BigDecimal maxDrawdownRate,
                                            List<String> warnings) {}
