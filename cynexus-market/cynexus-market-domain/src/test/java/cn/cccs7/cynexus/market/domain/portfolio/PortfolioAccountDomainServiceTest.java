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

        assertEquals(BigDecimal.ZERO, service.get("user", "tenant").availableCash());
    }

    @Test
    void normalizesAndPersistsNonNegativeCash() {
        InMemoryRepository repository = new InMemoryRepository(null);
        PortfolioAccountDomainService service = new PortfolioAccountDomainService(repository);

        PortfolioAccountBO result = service.updateAvailableCash("user", "tenant", new BigDecimal("123.456789123"));

        assertEquals(new BigDecimal("123.45678912"), result.availableCash());
        assertEquals(result, repository.value);
    }

    @Test
    void rejectsNegativeCash() {
        PortfolioAccountDomainService service = new PortfolioAccountDomainService(new InMemoryRepository(null));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateAvailableCash("user", "tenant", new BigDecimal("-0.01")));
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
        public PortfolioAccountBO save(String principalKey, String tenantKey, BigDecimal availableCash,
                                       OffsetDateTime updatedAt) {
            value = new PortfolioAccountBO(availableCash, updatedAt);
            return value;
        }
    }
}
