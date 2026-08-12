package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Resolves the configured per-trade commission when an imported trade has no fee. */
public final class PortfolioTradingFeePolicy {
    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");
    private static final BigDecimal RATE_PER_TEN_THOUSAND = new BigDecimal("0.854");
    private static final BigDecimal MAX_REASONABLE_FEE_RATE = new BigDecimal("0.01");
    private static final BigDecimal ETF_MINIMUM_FEE = new BigDecimal("0.1");
    private static final BigDecimal STOCK_MINIMUM_FEE = new BigDecimal("0.5");
    private static final int SCALE = 8;

    private PortfolioTradingFeePolicy() {
    }

    public static BigDecimal resolve(PortfolioInstrumentType instrumentType,
                                     BigDecimal quantity,
                                     BigDecimal price,
                                     BigDecimal declaredFee) {
        BigDecimal grossAmount = quantity.multiply(price);
        // Vision models can copy the 成交额 column into fee.
        if (declaredFee != null && declaredFee.signum() > 0
                && declaredFee.compareTo(grossAmount.multiply(MAX_REASONABLE_FEE_RATE)) <= 0) {
            return declaredFee;
        }
        if (grossAmount.compareTo(TEN_THOUSAND) < 0) {
            return instrumentType == PortfolioInstrumentType.ETF ? ETF_MINIMUM_FEE : STOCK_MINIMUM_FEE;
        }
        return grossAmount.multiply(RATE_PER_TEN_THOUSAND)
                .divide(TEN_THOUSAND, SCALE, RoundingMode.HALF_UP);
    }
}
