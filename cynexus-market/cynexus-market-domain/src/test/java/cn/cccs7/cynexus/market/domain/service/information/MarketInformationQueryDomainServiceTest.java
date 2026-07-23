package cn.cccs7.cynexus.market.domain.service.information;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.cccs7.cynexus.market.domain.entity.MarketInformationBO;
import cn.cccs7.cynexus.market.domain.repository.MarketInformationRepository;
import cn.cccs7.cynexus.market.domain.service.information.impl.MarketInformationQueryDomainServiceImpl;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MarketInformationQueryDomainServiceTest {

    @Test
    void normalizesFeedPageBoundsBeforeRepositoryCall() {
        AtomicInteger page = new AtomicInteger();
        AtomicInteger pageSize = new AtomicInteger();
        MarketInformationRepository repository = new FakeRepository() {
            @Override
            public MarketInformationBO.Feed feed(int requestedPage, int requestedPageSize,
                                                 String industry, String status) {
                page.set(requestedPage);
                pageSize.set(requestedPageSize);
                return null;
            }
        };

        new MarketInformationQueryDomainServiceImpl(repository).feed(0, 999, null, null);

        assertEquals(1, page.get());
        assertEquals(60, pageSize.get());
    }

    @Test
    void emptyAnalysisStatusRequestsDoNotReachRepository() {
        AtomicInteger calls = new AtomicInteger();
        MarketInformationRepository repository = new FakeRepository() {
            @Override
            public List<MarketInformationBO.News> analysisStatuses(List<Long> ids) {
                calls.incrementAndGet();
                return List.of();
            }
        };

        assertEquals(List.of(), new MarketInformationQueryDomainServiceImpl(repository).analysisStatuses(List.of()));
        assertEquals(0, calls.get());
    }

    private static class FakeRepository implements MarketInformationRepository {
        @Override public MarketInformationBO.Feed feed(int page, int pageSize, String industry, String status) { return null; }
        @Override public MarketInformationBO.Brief latestBrief() { return null; }
        @Override public MarketInformationBO.EventDetail eventDetail(long clusterId) { return null; }
        @Override public List<MarketInformationBO.News> analysisStatuses(List<Long> clusterIds) { return List.of(); }
        @Override public boolean requestAnalysis(long clusterId, boolean force, String requestedBy) { return false; }
        @Override public int refreshPipeline() { return 0; }
    }
}
