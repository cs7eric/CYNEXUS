package cn.cccs7.cynexus.market.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.repository.StockChartQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketFeatureAppServiceTest {

    @Test
    void dailyFeatureIsCalculatedFromPersistedBars() {
        StockChartQueryRepository repository = mock(StockChartQueryRepository.class);
        when(repository.listDailyBars("stock", "600519", 520, "qfq")).thenReturn(List.of(
                bar("2026-06-25", "10", "100", null),
                bar("2026-06-26", "11", "200", "2.50")));
        when(repository.stockMeta("600519")).thenReturn(StockChartBO.StockMeta.builder().symbol("600519").name("贵州茅台").build());

        var result = new StockFeatureAppService(repository).getDailyFeature("600519", "2026-06-26");

        assertEquals("贵州茅台", result.getStock().getStockName());
        assertEquals(new BigDecimal("11"), result.getClosePrice());
        assertEquals(new BigDecimal("10.0000"), result.getChangePercent());
        assertEquals(new BigDecimal("2.0000"), result.getVolumeRatio());
        assertNull(result.getMainNetInflow());
    }

    @Test
    void missingIntradayBarsReturnNoDataInsteadOfSyntheticValues() {
        StockChartQueryRepository repository = mock(StockChartQueryRepository.class);
        when(repository.findLatestMinuteDate("stock", "600519", LocalDate.of(2026, 6, 26))).thenReturn(Optional.empty());

        var result = new StockFeatureAppService(repository).getIntradayFeature("600519", "2026-06-26");

        assertNull(result);
    }

    private StockChartBO.Bar bar(String date, String close, String volume, String turnoverRate) {
        return StockChartBO.Bar.builder().tradeDate(LocalDate.parse(date)).close(new BigDecimal(close))
                .open(new BigDecimal(close)).high(new BigDecimal(close)).low(new BigDecimal(close))
                .volume(new BigDecimal(volume)).turnoverRate(turnoverRate == null ? null : new BigDecimal(turnoverRate)).build();
    }
}
