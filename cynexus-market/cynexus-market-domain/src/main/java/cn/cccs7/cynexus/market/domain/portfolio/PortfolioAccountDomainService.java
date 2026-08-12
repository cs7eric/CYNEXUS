package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class PortfolioAccountDomainService {
    private static final int MONEY_SCALE = 8;
    private final PortfolioAccountRepository repository;

    public PortfolioAccountDomainService(PortfolioAccountRepository repository) {
        this.repository = repository;
    }

    public PortfolioAccountBO get(String principalKey, String tenantKey) {
        PortfolioAccountBO account = repository.find(principalKey, tenantKey);
        return account == null ? new PortfolioAccountBO(null, BigDecimal.ZERO, null) : account;
    }

    public PortfolioAccountBO updateTotalAssets(String principalKey, String tenantKey, BigDecimal totalAssets) {
        if (totalAssets == null || totalAssets.signum() < 0) {
            throw new IllegalArgumentException("total assets must be zero or greater");
        }
        BigDecimal normalized = totalAssets.setScale(MONEY_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
        return repository.save(principalKey, tenantKey, normalized, OffsetDateTime.now());
    }

    public BigDecimal totalAssets(PortfolioAccountBO account, BigDecimal marketValue) {
        return totalAssets(account, marketValue, BigDecimal.ZERO);
    }

    public BigDecimal totalAssets(PortfolioAccountBO account, BigDecimal marketValue, BigDecimal accumulatedPnl) {
        BigDecimal market = nonNegative(marketValue);
        if (account.totalAssets() == null) {
            return market.add(account.legacyAvailableCash());
        }
        return account.totalAssets().add(nonNull(accumulatedPnl));
    }

    public BigDecimal availableCash(PortfolioAccountBO account, BigDecimal marketValue) {
        return availableCash(account, marketValue, BigDecimal.ZERO);
    }

    public BigDecimal availableCash(PortfolioAccountBO account, BigDecimal marketValue, BigDecimal accumulatedPnl) {
        return totalAssets(account, marketValue, accumulatedPnl).subtract(nonNegative(marketValue));
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
