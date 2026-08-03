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
        return account == null ? new PortfolioAccountBO(BigDecimal.ZERO, null) : account;
    }

    public PortfolioAccountBO updateAvailableCash(String principalKey, String tenantKey, BigDecimal availableCash) {
        if (availableCash == null || availableCash.signum() < 0) {
            throw new IllegalArgumentException("available cash must be zero or greater");
        }
        BigDecimal normalized = availableCash.setScale(MONEY_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
        return repository.save(principalKey, tenantKey, normalized, OffsetDateTime.now());
    }
}
