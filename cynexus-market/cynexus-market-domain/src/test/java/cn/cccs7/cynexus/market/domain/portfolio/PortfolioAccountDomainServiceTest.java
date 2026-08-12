package cn.cccs7.cynexus.market.domain.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class PortfolioAccountDomainServiceTest {
    @Test
    void returnsZeroWhenAccountHasNotBeenConfigured() {
        PortfolioAccountDomainService service = new PortfolioAccountDomainService(new InMemoryRepository(null));

        PortfolioAccountBO account = service.get("user", "tenant");
        assertEquals(BigDecimal.ZERO, account.legacyAvailableCash());
        assertEquals(null, account.totalAssets());
    }

    @Test
    void normalizesAndPersistsNonNegativeTotalAssets() {
        InMemoryRepository repository = new InMemoryRepository(null);
        PortfolioAccountDomainService service = new PortfolioAccountDomainService(repository);

        PortfolioAccountBO result = service.updateTotalAssets("user", "tenant", new BigDecimal("123.456789123"));

        assertEquals(new BigDecimal("123.45678912"), result.totalAssets());
        assertEquals(result, repository.value);
    }

    @Test
    void rejectsNegativeTotalAssets() {
        PortfolioAccountDomainService service = new PortfolioAccountDomainService(new InMemoryRepository(null));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateTotalAssets("user", "tenant", new BigDecimal("-0.01")));
    }

    @Test
    void derivesAvailableCashFromTotalAssetsAndMarketValue() {
        PortfolioAccountDomainService service = new PortfolioAccountDomainService(
                new InMemoryRepository(new PortfolioAccountBO(new BigDecimal("10000"), BigDecimal.ZERO, null)));

        PortfolioAccountBO account = service.get("user", "tenant");

        assertEquals(new BigDecimal("10000"), service.totalAssets(account, new BigDecimal("7200")));
        assertEquals(new BigDecimal("2800"), service.availableCash(account, new BigDecimal("7200")));
    }

    @Test
    void addsAccumulatedPnlToTheManualAssetBaseline() {
        PortfolioAccountDomainService service = new PortfolioAccountDomainService(
                new InMemoryRepository(new PortfolioAccountBO(new BigDecimal("10000"), BigDecimal.ZERO, null)));

        PortfolioAccountBO account = service.get("user", "tenant");

        assertEquals(new BigDecimal("10050"), service.totalAssets(account, new BigDecimal("7200"), new BigDecimal("50")));
        assertEquals(new BigDecimal("2850"), service.availableCash(account, new BigDecimal("7200"), new BigDecimal("50")));
    }

    private static final class InMemoryRepository implements PortfolioAccountRepository {
        private PortfolioAccountBO value;

        private InMemoryRepository(PortfolioAccountBO value) {
            this.value = value;
        }

        @Override
        public PortfolioAccountBO find(String principalKey, String tenantKey) {
            return value;
        }

        @Override
        public PortfolioAccountBO save(String principalKey, String tenantKey, BigDecimal totalAssets,
                                       OffsetDateTime updatedAt) {
            value = new PortfolioAccountBO(totalAssets, BigDecimal.ZERO, updatedAt);
            return value;
        }
    }
}
