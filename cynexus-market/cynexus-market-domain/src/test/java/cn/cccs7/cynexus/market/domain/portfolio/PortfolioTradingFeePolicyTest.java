package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PortfolioTradingFeePolicyTest {
    @Test
    void usesInstrumentMinimumFeeBelowTenThousand() {
        assertEquals(0, new BigDecimal("0.1").compareTo(
                PortfolioTradingFeePolicy.resolve(PortfolioInstrumentType.ETF,
                        new BigDecimal("200"), new BigDecimal("0.84"), BigDecimal.ZERO)));
        assertEquals(0, new BigDecimal("0.5").compareTo(
                PortfolioTradingFeePolicy.resolve(PortfolioInstrumentType.STOCK,
                        new BigDecimal("100"), new BigDecimal("12.50"), BigDecimal.ZERO)));
    }

    @Test
    void usesPointEightFiveFourPerTenThousandAtOrAboveThreshold() {
        BigDecimal fee = PortfolioTradingFeePolicy.resolve(PortfolioInstrumentType.ETF,
                new BigDecimal("10001"), BigDecimal.ONE, BigDecimal.ZERO);

        assertEquals(0, new BigDecimal("0.8540854").compareTo(fee));
    }

    @Test
    void keepsDeclaredFeeWhenItIsPresent() {
        BigDecimal fee = PortfolioTradingFeePolicy.resolve(PortfolioInstrumentType.ETF,
                new BigDecimal("200"), new BigDecimal("0.84"), new BigDecimal("0.12"));

        assertEquals(0, new BigDecimal("0.12").compareTo(fee));
    }

    @Test
    void replacesGrossAmountMisreadAsFee() {
        BigDecimal fee = PortfolioTradingFeePolicy.resolve(PortfolioInstrumentType.ETF,
                new BigDecimal("500"), new BigDecimal("0.872"), new BigDecimal("436.00"));

        assertEquals(0, new BigDecimal("0.1").compareTo(fee));
    }
}
