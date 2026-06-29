package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockChartQueryRepository {

    List<StockChartBO.SearchItem> search(String keyword, int limit);

    List<StockChartBO.Bar> listDailyBars(String targetType, String symbol, int limit);

    Optional<LocalDate> findLatestMinuteDate(String targetType, String symbol, LocalDate requestedDate);

    List<StockChartBO.Bar> listMinuteBars(String targetType, String symbol, LocalDate tradeDate, int periodMinutes);

    List<StockChartBO.IntradayPoint> listTimeshare(String targetType, String symbol, LocalDate tradeDate);

    StockChartBO.StockMeta stockMeta(String symbol);

    StockChartBO.FundFlowSnapshot fundFlow(String symbol);
}
