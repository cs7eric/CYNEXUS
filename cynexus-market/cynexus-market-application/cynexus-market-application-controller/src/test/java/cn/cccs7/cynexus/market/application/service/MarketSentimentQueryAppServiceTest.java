package cn.cccs7.cynexus.market.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.market.domain.entity.MaCountDailyBO;
import cn.cccs7.cynexus.market.domain.service.sentiment.MarketSentimentQueryDomainService;
import cn.cccs7.cynexus.market.domain.valueobject.sentiment.MarketSentimentHistoryResult;
import cn.cccs7.cynexus.market.domain.valueobject.sentiment.MarketSentimentQueryResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketSentimentQueryAppServiceTest {

    private final MarketSentimentQueryDomainService domainService = mock(MarketSentimentQueryDomainService.class);
    private final MarketSentimentQueryAppService appService = new MarketSentimentQueryAppService(domainService);

    @Test
    void mapsMetricFromDomainResultWithoutRepositoryAccess() {
        LocalDate requestedDate = LocalDate.of(2026, 7, 23);
        MaCountDailyBO row = MaCountDailyBO.builder()
                .tradeDate(requestedDate)
                .totalEligible(100)
                .aboveMa20(60)
                .source("java-pg")
                .build();
        when(domainService.maCount(requestedDate))
                .thenReturn(new MarketSentimentQueryResult<>(requestedDate, requestedDate, row, null));

        var result = appService.getMaCount(requestedDate);

        assertTrue(result.getOk());
        assertEquals(requestedDate, result.getTradeDate());
        assertEquals(100, result.getTotalEligible());
        assertEquals(60, result.getAboveMa20());
    }

    @Test
    void mapsHistoryRowsReturnedByDomainService() {
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 23);
        MaCountDailyBO row = MaCountDailyBO.builder()
                .tradeDate(end)
                .totalEligible(80)
                .breadthScore(new BigDecimal("0.42"))
                .build();
        when(domainService.maCountHistory(start, end)).thenReturn(new MarketSentimentHistoryResult<>(
                start, end, List.of(new MarketSentimentHistoryResult.Item<>(row, null)), "java-pg"));

        var result = appService.getMaCountHistory(start, end);

        assertTrue(result.getOk());
        assertEquals(1, result.getCount());
        assertEquals(end, result.getItems().getFirst().getTradeDate());
        assertEquals(80, result.getItems().getFirst().getTotalEligible());
    }

    @Test
    void returnsEmptyMetricUsingDomainEffectiveDate() {
        LocalDate requestedDate = LocalDate.of(2026, 7, 22);
        LocalDate effectiveDate = LocalDate.of(2026, 7, 21);
        when(domainService.maCount(requestedDate)).thenReturn(new MarketSentimentQueryResult<>(
                requestedDate, effectiveDate, null, null));

        var result = appService.getMaCount(requestedDate);

        assertFalse(result.getOk());
        assertEquals(effectiveDate, result.getTradeDate());
    }
}
