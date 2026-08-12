package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StockChartDTO;
import cn.cccs7.cynexus.market.application.service.StockChartQueryAppService;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/stock-chart")
public class StockChartQueryController {

    private final StockChartQueryAppService stockChartQueryAppService;

    public StockChartQueryController(StockChartQueryAppService stockChartQueryAppService) {
        this.stockChartQueryAppService = stockChartQueryAppService;
    }

    @GetMapping("/search")
    public Result<StockChartDTO.SearchResponseDTO> search(@RequestParam("q") String keyword) {
        return Result.success(stockChartQueryAppService.search(keyword));
    }

    @GetMapping("/klines")
    public Result<StockChartDTO.KlineResponseDTO> klines(@RequestParam(name = "target_type", defaultValue = "stock") String targetType,
                                                         @RequestParam String symbol,
                                                         @RequestParam(name = "period", defaultValue = "1d") String period,
                                                         @RequestParam(name = "adjust", defaultValue = "qfq") String adjust) {
        return Result.success(stockChartQueryAppService.klines(targetType, symbol, period, adjust));
    }

    @GetMapping("/stock-meta")
    public Result<StockChartDTO.StockMetaDTO> stockMeta(@RequestParam String symbol) {
        return Result.success(stockChartQueryAppService.stockMeta(symbol));
    }

    @GetMapping("/stock-sectors")
    public Result<StockChartDTO.StockSectorsResponseDTO> stockSectors(@RequestParam String symbol,
                                                                      @RequestParam(name = "trade_date", required = false) String tradeDate) {
        return Result.success(stockChartQueryAppService.stockSectors(symbol, tradeDate));
    }

    @GetMapping("/sector-stocks")
    public Result<StockChartDTO.SectorStocksResponseDTO> sectorStocks(@RequestParam(name = "sector_id", required = false) String sectorId,
                                                                      @RequestParam(name = "category_raw", required = false) Integer categoryRaw,
                                                                      @RequestParam(name = "topic_id", required = false) String topicId,
                                                                      @RequestParam(required = false) String name,
                                                                      @RequestParam(defaultValue = "200") int limit) {
        return Result.success(stockChartQueryAppService.sectorStocks(sectorId, categoryRaw, topicId, name, limit));
    }

    @GetMapping("/fund-flow")
    public Result<StockChartDTO.FundFlowSnapshotDTO> fundFlow(@RequestParam String symbol) {
        return Result.success(stockChartQueryAppService.fundFlow(symbol));
    }

    @GetMapping("/intraday")
    public Result<StockChartDTO.IntradayDTO> intraday(@RequestParam(name = "target_type", defaultValue = "stock") String targetType,
                                                      @RequestParam String symbol,
                                                      @RequestParam(required = false) String name,
                                                      @RequestParam(name = "adjust", defaultValue = "qfq") String adjust,
                                                      @RequestParam(name = "trade_date", required = false) String tradeDate,
                                                      @RequestParam(name = "periods", defaultValue = "1m,5m,15m,30m") String periods,
                                                      @RequestParam(name = "strict_date", defaultValue = "false") boolean strictDate) {
        List<String> periodList = Arrays.stream(periods.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
        return Result.success(stockChartQueryAppService.intraday(targetType, symbol, name, adjust, tradeDate, periodList, strictDate));
    }

    @GetMapping("/auction")
    public Result<StockChartDTO.AuctionDTO> auction(@RequestParam String symbol,
                                                    @RequestParam(name = "trade_date", required = false) String tradeDate) {
        return Result.success(stockChartQueryAppService.auction(symbol, tradeDate));
    }

    @GetMapping("/auction/history")
    public Result<StockChartDTO.AuctionHistoryResponseDTO> auctionHistory(@RequestParam String symbol,
                                                                          @RequestParam(defaultValue = "30") int limit) {
        return Result.success(stockChartQueryAppService.auctionHistory(symbol, limit));
    }

    @GetMapping("/f10/valuation")
    public Result<StockChartDTO.F10ValuationDTO> valuation(@RequestParam String symbol,
                                                           @RequestParam(name = "req_id", defaultValue = "200191") String reqId) {
        return Result.success(stockChartQueryAppService.valuation(symbol, reqId));
    }

    @GetMapping("/f10/finance-report")
    public Result<StockChartDTO.F10FinanceReportDTO> financeReport(@RequestParam String symbol,
                                                                   @RequestParam(name = "report_type", defaultValue = "zcfzb") String reportType) {
        return Result.success(stockChartQueryAppService.financeReport(symbol, reportType));
    }

    @GetMapping("/f10/business-composition")
    public Result<StockChartDTO.F10BusinessCompositionDTO> businessComposition(@RequestParam String symbol,
                                                                               @RequestParam(name = "report_date", required = false) String reportDate) {
        return Result.success(stockChartQueryAppService.businessComposition(symbol, reportDate));
    }

    @GetMapping("/f10/profit-forecast")
    public Result<StockChartDTO.F10ProfitForecastDTO> profitForecast(@RequestParam String symbol) {
        return Result.success(stockChartQueryAppService.profitForecast(symbol));
    }

    @GetMapping("/f10/announcements")
    public Result<StockChartDTO.F10AnnouncementsDTO> announcements(@RequestParam String symbol) {
        return Result.success(stockChartQueryAppService.announcements(symbol));
    }

    @GetMapping("/f10/news")
    public Result<StockChartDTO.F10NewsDTO> news(@RequestParam String symbol) {
        return Result.success(stockChartQueryAppService.news(symbol));
    }

    @GetMapping("/f10/roadshows")
    public Result<StockChartDTO.F10RoadshowsDTO> roadshows(@RequestParam String symbol) {
        return Result.success(stockChartQueryAppService.roadshows(symbol));
    }

    @GetMapping("/f10/company-news")
    public Result<StockChartDTO.F10CompanyNewsDTO> companyNews(@RequestParam String symbol,
                                                               @RequestParam(defaultValue = "gsyj") String section) {
        return Result.success(stockChartQueryAppService.companyNews(symbol, section));
    }
}
