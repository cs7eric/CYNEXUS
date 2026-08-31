package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioAccountSnapshotBO(Long id, LocalDate snapshotDate, BigDecimal totalAssets,
                                         BigDecimal availableCash, BigDecimal marketValue,
                                         BigDecimal netDeposit, BigDecimal netWithdrawal,
                                         BigDecimal dailyPnl, BigDecimal dailyReturn) {
    public PortfolioAccountSnapshotBO {
        if (snapshotDate == null || totalAssets == null || totalAssets.signum() < 0
                || availableCash == null || availableCash.signum() < 0
                || marketValue == null || marketValue.signum() < 0) {
            throw new IllegalArgumentException("invalid portfolio account snapshot");
        }
        netDeposit = netDeposit == null ? BigDecimal.ZERO : netDeposit;
        netWithdrawal = netWithdrawal == null ? BigDecimal.ZERO : netWithdrawal;
    }
}
