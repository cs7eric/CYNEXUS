package cn.cccs7.cynexus.market.domain.service.etf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.cccs7.cynexus.market.domain.entity.MarketEtfDataBO;
import cn.cccs7.cynexus.market.domain.repository.MarketEtfQueryRepository;
import cn.cccs7.cynexus.market.domain.etf.MarketEtfDomainService;
import cn.cccs7.cynexus.market.domain.service.etf.impl.MarketEtfQueryDomainServiceImpl;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MarketEtfQueryDomainServiceTest {

    @Test
    void clampsHistoryAndRowsBeforeLoading() {
        AtomicInteger historyDays = new AtomicInteger();
        AtomicInteger rowLimit = new AtomicInteger();
        MarketEtfQueryRepository repository = (history, rows) -> {
            historyDays.set(history);
            rowLimit.set(rows);
            return MarketEtfDataBO.builder().source("test").spots(List.of()).build();
        };

        new MarketEtfQueryDomainServiceImpl(repository, new MarketEtfDomainService()).getMarket(1, 999);

        assertEquals(3, historyDays.get());
        assertEquals(300, rowLimit.get());
    }

    @Test
    void delegatesBuiltMarketResultToDomainRules() {
        MarketEtfDataBO data = MarketEtfDataBO.builder().source("test").spots(List.of()).build();
        MarketEtfQueryDomainService service = new MarketEtfQueryDomainServiceImpl((history, rows) -> data,
                new MarketEtfDomainService());

        assertEquals(false, service.getMarket(10, 20).getOk());
        assertEquals(data.getSource(), service.getMarket(10, 20).getSource());
    }
}
