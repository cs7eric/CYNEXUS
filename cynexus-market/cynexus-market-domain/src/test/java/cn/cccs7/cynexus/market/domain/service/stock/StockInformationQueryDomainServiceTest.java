package cn.cccs7.cynexus.market.domain.service.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;
import cn.cccs7.cynexus.market.domain.repository.StockInformationRepository;
import cn.cccs7.cynexus.market.domain.service.stock.impl.StockInformationQueryDomainServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockInformationQueryDomainServiceTest {

    @Test
    void normalizesSymbolAndLimit() {
        String[] symbol = new String[1];
        int[] limit = new int[1];
        StockInformationRepository repository = new FakeRepository() {
            @Override
            public StockInformationBO.Feed feed(String requestedSymbol, int requestedLimit) {
                symbol[0] = requestedSymbol;
                limit[0] = requestedLimit;
                return StockInformationBO.Feed.builder().symbol(requestedSymbol).build();
            }
        };

        new StockInformationQueryDomainServiceImpl(repository).feed(" 600519 ", 999);

        assertEquals("600519", symbol[0]);
        assertEquals(100, limit[0]);
    }

    @Test
    void rejectsBlankSymbol() {
        assertThrows(IllegalArgumentException.class,
                () -> new StockInformationQueryDomainServiceImpl(new FakeRepository()).feed(" ", 10));
    }

    private static class FakeRepository implements StockInformationRepository {
        @Override public StockInformationBO.Feed feed(String symbol, int limit) { return StockInformationBO.Feed.builder().build(); }
        @Override public StockInformationBO.SourceRecord findSourceRecord(String symbol, String kind, String source, String itemId) { return null; }
        @Override public StockInformationBO.Content findContent(StockInformationBO.SourceRecord sourceRecord) { return null; }
        @Override public StockInformationBO.Content saveContent(StockInformationBO.Content content) { return content; }
        @Override public StockInformationBO.Analysis findLatestAnalysis(Long contentId, String contentHash, String schemaVersion, String promptVersion) { return null; }
        @Override public StockInformationBO.Analysis saveAnalysis(StockInformationBO.Analysis analysis) { return analysis; }
    }
}
