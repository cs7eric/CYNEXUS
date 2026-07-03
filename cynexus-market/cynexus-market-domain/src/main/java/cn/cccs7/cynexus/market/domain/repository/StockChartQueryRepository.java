package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockChartQueryRepository {

    List<StockChartBO.SearchItem> search(String keyword, int limit);

    List<StockChartBO.Bar> listDailyBars(String targetType, String symbol, int limit, String adjust);

    Optional<LocalDate> findLatestMinuteDate(String targetType, String symbol, LocalDate requestedDate);

    List<StockChartBO.Bar> listMinuteBars(String targetType, String symbol, LocalDate tradeDate, int periodMinutes);

    List<StockChartBO.IntradayPoint> listTimeshare(String targetType, String symbol, LocalDate tradeDate);

    Optional<LocalDate> findLatestAuctionDate(String targetType, String symbol, LocalDate requestedDate);

    List<StockChartBO.AuctionPoint> listAuctionPoints(String targetType, String symbol, LocalDate tradeDate);

    StockChartBO.StockMeta stockMeta(String symbol);

    StockChartBO.StockSectors stockSectors(String symbol);

    StockChartBO.SectorStocks sectorStocks(String sectorId, Integer categoryRaw, String topicId, String name, int limit);

    StockChartBO.FundFlowSnapshot fundFlow(String symbol);

    StockChartBO.F10Valuation valuation(String symbol, String reqId);

    StockChartBO.F10FinanceReport financeReport(String symbol, String reportType);

    StockChartBO.F10BusinessComposition businessComposition(String symbol, String reportDate);

    StockChartBO.F10ProfitForecast profitForecast(String symbol);

    StockChartBO.F10Announcements announcements(String symbol);

    StockChartBO.F10News news(String symbol);

    StockChartBO.F10Roadshows roadshows(String symbol);

    StockChartBO.F10CompanyNews companyNews(String symbol, String section);
}
