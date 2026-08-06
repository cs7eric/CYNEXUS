package cn.cccs7.cynexus.market.infra.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.market.domain.entity.MarginIndustrySummaryBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustryConstituentPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustrySummaryPO;
import cn.cccs7.cynexus.market.infra.basic.service.MarginTradingBasicService;
import cn.cccs7.cynexus.market.infra.client.ClickHouseStockChartClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarginTradingRepositoryImplTest {

    @Test
    void usesDailyKAmountsForIndustryTurnover() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 3);
        MarginTradingBasicService basicService = mock(MarginTradingBasicService.class);
        ClickHouseStockChartClient clickHouseClient = mock(ClickHouseStockChartClient.class);
        MarginTradingRepositoryImpl repository = new MarginTradingRepositoryImpl(basicService, clickHouseClient);

        MarginIndustrySummaryPO summary = new MarginIndustrySummaryPO();
        summary.setIndustryCode("881121");
        MarginIndustryConstituentPO first = constituent("881121", "000001");
        MarginIndustryConstituentPO second = constituent("881121", "600000");
        when(basicService.listIndustrySummaries(tradeDate, "margin_balance", "desc", 0, 20))
                .thenReturn(List.of(summary));
        when(basicService.listIndustryConstituents(tradeDate, List.of("881121")))
                .thenReturn(List.of(first, second));
        when(clickHouseClient.listDailyAmounts(anyList(), eq(tradeDate), eq(tradeDate)))
                .thenReturn(List.of(
                        new ClickHouseStockChartClient.DailyAmount(
                                "000001", tradeDate, new BigDecimal("5000000000")
                        ),
                        new ClickHouseStockChartClient.DailyAmount(
                                "600000", tradeDate, new BigDecimal("7000000000")
                        )
                ));

        List<MarginIndustrySummaryBO> result = repository.listIndustrySummaries(
                tradeDate, "margin_balance", "desc", 0, 20
        );

        assertEquals(new BigDecimal("12000000000"), result.getFirst().getTurnoverAmount());
    }

    private MarginIndustryConstituentPO constituent(String industryCode, String stockCode) {
        MarginIndustryConstituentPO constituent = new MarginIndustryConstituentPO();
        constituent.setIndustryCode(industryCode);
        constituent.setStockCode(stockCode);
        return constituent;
    }
}
