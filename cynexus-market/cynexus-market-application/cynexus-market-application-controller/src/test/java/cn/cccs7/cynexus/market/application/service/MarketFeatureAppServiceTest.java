package cn.cccs7.cynexus.market.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import cn.cccs7.cynexus.market.domain.entity.StockIntradayFeatureBO;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import cn.cccs7.cynexus.market.domain.valueobject.Stock;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketFeatureAppServiceTest {

    @Test
    void dailyFeatureIsCalculatedFromPersistedBars() {
        StockChartQueryDomainService domainService = mock(StockChartQueryDomainService.class);
        when(domainService.dailyFeature("600519", "2026-06-26")).thenReturn(StockDailyFeatureBO.builder()
                .stock(new Stock("600519", "贵州茅台")).tradeDate("2026-06-26")
                .closePrice(new BigDecimal("11")).changePercent(new BigDecimal("10.0000"))
                .volumeRatio(new BigDecimal("2.0000")).build());

        var result = new StockFeatureAppService(domainService).getDailyFeature("600519", "2026-06-26");

        assertEquals("贵州茅台", result.getStock().getStockName());
        assertEquals(new BigDecimal("11"), result.getClosePrice());
        assertEquals(new BigDecimal("10.0000"), result.getChangePercent());
        assertEquals(new BigDecimal("2.0000"), result.getVolumeRatio());
        assertNull(result.getMainNetInflow());
    }

    @Test
    void missingIntradayBarsReturnNoDataInsteadOfSyntheticValues() {
        StockChartQueryDomainService domainService = mock(StockChartQueryDomainService.class);
        when(domainService.intradayFeature("600519", "2026-06-26")).thenReturn(null);

        var result = new StockFeatureAppService(domainService).getIntradayFeature("600519", "2026-06-26");

        assertNull(result);
    }

}
