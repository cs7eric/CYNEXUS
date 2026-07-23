package cn.cccs7.cynexus.market.domain.service.pulse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.repository.MarketOverviewRepository;
import cn.cccs7.cynexus.market.domain.service.pulse.impl.MarketOverviewQueryDomainServiceImpl;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MarketOverviewQueryDomainServiceTest {

    @Test
    void trimsTradeDateBeforeLookup() {
        String[] captured = new String[1];
        MarketOverviewRepository repository = new FakeRepository() {
            @Override
            public Optional<MarketOverviewBO> findByTradeDate(String tradeDate) {
                captured[0] = tradeDate;
                return Optional.of(MarketOverviewBO.builder().tradeDate(LocalDate.parse(tradeDate)).build());
            }
        };

        assertEquals(LocalDate.parse("2026-07-23"), new MarketOverviewQueryDomainServiceImpl(repository)
                .getOverview(" 2026-07-23 ").getTradeDate());
        assertEquals("2026-07-23", captured[0]);
    }

    @Test
    void rejectsBlankTradeDate() {
        assertThrows(IllegalArgumentException.class,
                () -> new MarketOverviewQueryDomainServiceImpl(new FakeRepository()).getOverview(" "));
    }

    private static class FakeRepository implements MarketOverviewRepository {
        @Override public Optional<MarketOverviewBO> findByTradeDate(String tradeDate) { return Optional.empty(); }
        @Override public List<MarketOverviewBO> listAll() { return List.of(); }
        @Override public MarketOverviewBO findById(Long id) { return null; }
        @Override public Long create(MarketOverviewBO value) { return null; }
        @Override public boolean update(MarketOverviewBO value) { return false; }
        @Override public boolean delete(Long id) { return false; }
    }
}
