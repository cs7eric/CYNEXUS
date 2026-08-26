package cn.cccs7.cynexus.market.domain.factorresearch;

import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorReplayBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustrySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.IndexMinuteBarBO;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.repository.MarketIndicatorRepository;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import cn.cccs7.cynexus.market.domain.service.indicator.MarketIndicatorQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventReplayDomainServiceImplTest {
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 14);

    @Test
    void excludesBarsAfterAnchorAndReportsPartialCoverage() {
        OffsetDateTime anchor = at("10:00");
        List<StockChartBO.Bar> bars = List.of(bar("09:59"), bar("10:00"), bar("10:01"),
                StockChartBO.Bar.builder().tradeDate(TRADE_DATE).build());
        EventReplayDomainService service = new EventReplayDomainServiceImpl(stockService(bars), indicatorService(false));

        EventReplayBO.Result result = service.replay(new EventReplayBO.Query("600000", TRADE_DATE, anchor, 120));

        assertEquals("PARTIAL", result.status());
        assertEquals(4, result.coverage().stockBarsRead());
        assertEquals(2, result.coverage().stockBarsIncluded());
        assertEquals(2, result.stockMinuteBars().size());
        assertEquals(anchor.toInstant().toEpochMilli(), result.dataCutoffAt().toInstant().toEpochMilli());
    }

    @Test
    void rejectsAnAnchorFromAnotherTradingDay() {
        EventReplayDomainService service = new EventReplayDomainServiceImpl(stockService(List.of()), indicatorService(true));

        assertThrows(IllegalArgumentException.class,
                () -> service.replay(new EventReplayBO.Query("600000", TRADE_DATE, at("10:00").plusDays(1), 120)));
    }

    private StockChartQueryDomainService stockService(List<StockChartBO.Bar> bars) {
        return (StockChartQueryDomainService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{StockChartQueryDomainService.class}, (proxy, method, args) -> {
                    if ("intraday".equals(method.getName())) {
                        return new StockChartQueryDomainService.IntradayResult("600000", "stock", null, "none", "none", "none",
                                "clickhouse.ch_dwd_kline_1m", TRADE_DATE, TRADE_DATE, null, List.of(), Map.of("1m", bars), null);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private MarketIndicatorQueryDomainService indicatorService(boolean industryAvailable) {
        MarketIndicatorRepository indicatorRepository = (MarketIndicatorRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{MarketIndicatorRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "listReplayEvents" -> List.of();
                    case "listReplayIndustries" -> industryAvailable
                            ? List.of(MarketIndicatorIndustrySnapshotBO.builder().industryCode("I1").build()) : List.of();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        MarketPulseQueryRepository pulseRepository = (MarketPulseQueryRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{MarketPulseQueryRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "findLatestIndexMinuteDate" -> Optional.of(TRADE_DATE);
                    case "listIndexMinuteBars" -> List.of(IndexMinuteBarBO.builder().timestamp(at("10:00").toInstant().toEpochMilli()).build());
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new MarketIndicatorQueryDomainService(indicatorRepository, pulseRepository);
    }

    private StockChartBO.Bar bar(String time) {
        return StockChartBO.Bar.builder().tradeDate(TRADE_DATE)
                .timestamp(at(time).toInstant().toEpochMilli()).build();
    }

    private OffsetDateTime at(String time) {
        return TRADE_DATE.atTime(LocalTime.parse(time)).atZone(MARKET_ZONE).toOffsetDateTime();
    }
}
