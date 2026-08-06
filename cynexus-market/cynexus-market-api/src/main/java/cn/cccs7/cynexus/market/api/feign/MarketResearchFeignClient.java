package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.AStockSkillDataDTO;
import cn.cccs7.cynexus.market.api.dto.DataSourceFreshnessDTO;
import cn.cccs7.cynexus.market.api.dto.IndustryApplicationDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingIndustriesDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingIndustryHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingLatestDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingStockHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingStocksDTO;
import cn.cccs7.cynexus.market.api.dto.FuturesPositionDTO;
import cn.cccs7.cynexus.market.api.dto.MarketEtfDTO;
import cn.cccs7.cynexus.market.api.dto.MarketIndicatorDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseHeatmapDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseIndexKlineBatchDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseIndexReturnsDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseIndexReturnsHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseManualFundFlowDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewEltdxDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseSectorSnapshotDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseSectorSnapshotHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseStyleSectorConstituentsDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseStyleSectorListDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentLimitEmotionSummaryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentIndexDailyDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentMaCountDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentMsiIndexDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentProfitEffectDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentRiskAppetiteDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentSectorBreadthDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentStyleRiskDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentTurnoverDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentVolatilityDTO;
import cn.cccs7.cynexus.market.api.dto.StockChartDTO;
import cn.cccs7.cynexus.market.api.dto.StockInformationContentDTO;
import cn.cccs7.cynexus.market.api.dto.StockInformationDTO;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Read-only market query contract used by AI adapters and other internal consumers. */
@FeignClient(name = "cynexus-market", contextId = "marketResearchFeignClient",
        url = "${CYNEXUS_AI_MARKET_BASE_URL:}", path = "/api/market")
public interface MarketResearchFeignClient {

    @GetMapping("/stock-chart/search")
    Result<StockChartDTO.SearchResponseDTO> searchStocks(@RequestParam("q") String keyword);

    @GetMapping("/stock-chart/stock-meta")
    Result<StockChartDTO.StockMetaDTO> getStockMeta(@RequestParam("symbol") String symbol);

    @GetMapping("/stock-chart/stock-sectors")
    Result<StockChartDTO.StockSectorsResponseDTO> getStockSectors(@RequestParam("symbol") String symbol);

    @GetMapping("/stock-chart/sector-stocks")
    Result<StockChartDTO.SectorStocksResponseDTO> getSectorStocks(
            @RequestParam(value = "sector_id", required = false) String sectorId,
            @RequestParam(value = "category_raw", required = false) Integer categoryRaw,
            @RequestParam(value = "topic_id", required = false) String topicId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam("limit") int limit);

    @GetMapping("/stock-chart/fund-flow")
    Result<StockChartDTO.FundFlowSnapshotDTO> getStockFundFlow(@RequestParam("symbol") String symbol);

    @GetMapping("/stock-chart/intraday")
    Result<StockChartDTO.IntradayDTO> getStockIntraday(
            @RequestParam("target_type") String targetType,
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam("adjust") String adjust,
            @RequestParam(value = "trade_date", required = false) String tradeDate,
            @RequestParam("periods") String periods);

    @GetMapping("/stock-chart/auction")
    Result<StockChartDTO.AuctionDTO> getStockAuction(
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "trade_date", required = false) String tradeDate);

    @GetMapping("/stock-chart/auction/history")
    Result<StockChartDTO.AuctionHistoryResponseDTO> getStockAuctionHistory(
            @RequestParam("symbol") String symbol,
            @RequestParam("limit") Integer limit);

    @GetMapping("/stock-chart/f10/valuation")
    Result<StockChartDTO.F10ValuationDTO> getValuation(@RequestParam("symbol") String symbol,
                                                        @RequestParam("req_id") String requestId);

    @GetMapping("/stock-chart/f10/finance-report")
    Result<StockChartDTO.F10FinanceReportDTO> getFinanceReport(@RequestParam("symbol") String symbol,
                                                                @RequestParam("report_type") String reportType);

    @GetMapping("/stock-chart/f10/business-composition")
    Result<StockChartDTO.F10BusinessCompositionDTO> getBusinessComposition(
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "report_date", required = false) String reportDate);

    @GetMapping("/stock-chart/f10/profit-forecast")
    Result<StockChartDTO.F10ProfitForecastDTO> getProfitForecast(@RequestParam("symbol") String symbol);

    @GetMapping("/stock-chart/f10/announcements")
    Result<StockChartDTO.F10AnnouncementsDTO> getAnnouncements(@RequestParam("symbol") String symbol);

    @GetMapping("/stock-chart/f10/news")
    Result<StockChartDTO.F10NewsDTO> getNews(@RequestParam("symbol") String symbol);

    @GetMapping("/stock-chart/f10/roadshows")
    Result<StockChartDTO.F10RoadshowsDTO> getRoadshows(@RequestParam("symbol") String symbol);

    @GetMapping("/stock-chart/f10/company-news")
    Result<StockChartDTO.F10CompanyNewsDTO> getCompanyNews(@RequestParam("symbol") String symbol,
                                                            @RequestParam("section") String section);

    @GetMapping("/sentiment/ma-count")
    Result<MarketSentimentMaCountDTO> getMaCount(@RequestParam(value = "date", required = false) String date);

    @GetMapping("/sentiment/risk-appetite")
    Result<MarketSentimentRiskAppetiteDTO> getRiskAppetite(@RequestParam(value = "date", required = false) String date);

    @GetMapping("/sentiment/limit-emotion-summary")
    Result<MarketSentimentLimitEmotionSummaryDTO> getLimitEmotion(@RequestParam(value = "date", required = false) String date);

    @GetMapping("/sentiment/volatility-sentiment")
    Result<MarketSentimentVolatilityDTO> getVolatility(@RequestParam(value = "date", required = false) String date);

    @GetMapping("/sentiment/turnover-activity")
    Result<MarketSentimentTurnoverDTO> getTurnover(@RequestParam(value = "date", required = false) String date);

    @GetMapping("/sentiment/style-risk-appetite")
    Result<MarketSentimentStyleRiskDTO> getStyleRisk(@RequestParam(value = "date", required = false) String date);

    @GetMapping("/sentiment/profit-effect")
    Result<MarketSentimentProfitEffectDTO> getProfitEffect(@RequestParam(value = "date", required = false) String date);

    @GetMapping("/sentiment/sector-breadth")
    Result<MarketSentimentSectorBreadthDTO> getSectorBreadth(@RequestParam(value = "date", required = false) String date);

    @GetMapping("/sentiment/index")
    Result<MarketSentimentMsiIndexDTO> getMsiIndex(@RequestParam(value = "date", required = false) String date);

    @GetMapping("/sentiment/ma-count/history")
    Result<MarketSentimentMaCountDTO.HistoryResponseDTO> getMaCountHistory(@RequestParam("start") String start,
                                                                            @RequestParam("end") String end);

    @GetMapping("/sentiment/risk-appetite/history")
    Result<MarketSentimentRiskAppetiteDTO.HistoryResponseDTO> getRiskAppetiteHistory(@RequestParam("start") String start,
                                                                                      @RequestParam("end") String end);

    @GetMapping("/sentiment/limit-emotion-summary/history")
    Result<MarketSentimentLimitEmotionSummaryDTO.HistoryResponseDTO> getLimitEmotionHistory(@RequestParam("start") String start,
                                                                                              @RequestParam("end") String end);

    @GetMapping("/sentiment/volatility-sentiment/history")
    Result<MarketSentimentVolatilityDTO.HistoryResponseDTO> getVolatilityHistory(@RequestParam("start") String start,
                                                                                   @RequestParam("end") String end);

    @GetMapping("/sentiment/turnover-activity/history")
    Result<MarketSentimentTurnoverDTO.HistoryResponseDTO> getTurnoverHistory(@RequestParam("start") String start,
                                                                              @RequestParam("end") String end);

    @GetMapping("/sentiment/style-risk-appetite/history")
    Result<MarketSentimentStyleRiskDTO.HistoryResponseDTO> getStyleRiskHistory(@RequestParam("start") String start,
                                                                                 @RequestParam("end") String end);

    @GetMapping("/sentiment/profit-effect/history")
    Result<MarketSentimentProfitEffectDTO.HistoryResponseDTO> getProfitEffectHistory(@RequestParam("start") String start,
                                                                                       @RequestParam("end") String end);

    @GetMapping("/sentiment/sector-breadth/history")
    Result<MarketSentimentSectorBreadthDTO.HistoryResponseDTO> getSectorBreadthHistory(
            @RequestParam("days") int days, @RequestParam(value = "end", required = false) String end);

    @GetMapping("/sentiment/index/history")
    Result<MarketSentimentMsiIndexDTO.HistoryResponseDTO> getMsiIndexHistory(@RequestParam("start") String start,
                                                                              @RequestParam("end") String end);

    @GetMapping("/sentiment/index/daily")
    Result<MarketSentimentIndexDailyDTO> getIndexDaily(@RequestParam("code") String code,
                                                       @RequestParam("start") String start,
                                                       @RequestParam("end") String end);

    @GetMapping("/industry-application/overview")
    Result<IndustryApplicationDTO.OverviewResponse> getIndustryOverview(
            @RequestParam(value = "sort_by", required = false) String sortBy,
            @RequestParam("ascending") boolean ascending,
            @RequestParam("count") int count);

    @GetMapping("/industry-application/targets")
    Result<IndustryApplicationDTO.ConfigResponse> getIndustryTargets();

    @GetMapping("/industry-application/target-codes")
    Result<IndustryApplicationDTO.TargetCodesResponse> getIndustryTargetCodes(
            @RequestParam(value = "kind", required = false) String kind);

    @GetMapping("/industry-application/kline")
    Result<IndustryApplicationDTO.KlineResponse> getIndustryKline(
            @RequestParam("target_type") String targetType, @RequestParam("symbol") String symbol,
            @RequestParam("period") String period, @RequestParam("count") int count);

    @GetMapping("/industry-application/results/{targetId}")
    Result<IndustryApplicationDTO.ResultResponse> getIndustryResult(@PathVariable("targetId") String targetId);

    @GetMapping("/industry-application/fund-flow")
    Result<IndustryApplicationDTO.FundFlowResponse> getIndustryFundFlow(
            @RequestParam(value = "trade_date", required = false) String tradeDate,
            @RequestParam(value = "top", required = false) Integer top);

    @GetMapping("/industry-application/fund-flow/history")
    Result<IndustryApplicationDTO.FundFlowHistoryResponse> getIndustryFundFlowHistory(
            @RequestParam(value = "max_trade_date", required = false) String maxTradeDate,
            @RequestParam("days") int days,
            @RequestParam(value = "top", required = false) Integer top);

    @GetMapping("/industry-application/tdx-fund-flow")
    Result<IndustryApplicationDTO.TdxFundFlowResponse> getIndustryTdxFundFlow(
            @RequestParam("target_type") String targetType,
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "trade_date", required = false) String tradeDate,
            @RequestParam("days") int days);

    @GetMapping("/margin/latest")
    Result<MarginTradingLatestDTO> getMarginLatest(@RequestParam(value = "tradeDate", required = false) String tradeDate,
                                                    @RequestParam("top") int top);

    @GetMapping("/margin/summary/history")
    Result<MarginTradingHistoryDTO> getMarginSummaryHistory(@RequestParam(value = "start", required = false) String start,
                                                            @RequestParam(value = "end", required = false) String end);

    @GetMapping("/margin/stocks")
    Result<MarginTradingStocksDTO> getMarginStocks(
            @RequestParam(value = "tradeDate", required = false) String tradeDate,
            @RequestParam(value = "exchange", required = false) String exchange,
            @RequestParam(value = "industryCode", required = false) String industryCode,
            @RequestParam("sort") String sort,
            @RequestParam("sortDir") String sortDir,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam("limit") int limit);

    @GetMapping("/margin/stocks/{stockCode}/history")
    Result<MarginTradingStockHistoryDTO> getMarginStockHistory(@PathVariable("stockCode") String stockCode,
                                                                @RequestParam("limit") int limit);

    @GetMapping("/margin/industries")
    Result<MarginTradingIndustriesDTO> getMarginIndustries(
            @RequestParam(value = "tradeDate", required = false) String tradeDate,
            @RequestParam("sort") String sort,
            @RequestParam("sortDir") String sortDir,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @GetMapping("/margin/industries/{industryCode}/history")
    Result<MarginTradingIndustryHistoryDTO> getMarginIndustryHistory(
            @PathVariable("industryCode") String industryCode,
            @RequestParam(value = "tradeDate", required = false) String tradeDate,
            @RequestParam("limit") int limit);

    @GetMapping("/indicator/latest")
    Result<MarketIndicatorDTO.Overview> getMarketIndicatorLatest(
            @RequestParam(value = "tradeDate", required = false) String tradeDate,
            @RequestParam("status") String status,
            @RequestParam("eventType") String eventType,
            @RequestParam("limit") int limit);

    @GetMapping("/indicator/industry/detail")
    Result<MarketIndicatorDTO.IndustryDetail> getMarketIndicatorIndustryDetail(
            @RequestParam("industryCode") String industryCode,
            @RequestParam(value = "tradeDate", required = false) String tradeDate,
            @RequestParam("page") int page,
            @RequestParam("pageSize") int pageSize);

    @GetMapping("/indicator/transmission")
    Result<Map<String, Object>> getMarketIndicatorTransmission(
            @RequestParam(value = "tradeDate", required = false) String tradeDate,
            @RequestParam("limit") int limit);

    @GetMapping("/etf")
    Result<MarketEtfDTO> getMarketEtf(@RequestParam("historyDays") int historyDays,
                                      @RequestParam("rowLimit") int rowLimit);

    @GetMapping("/futures-position/latest")
    Result<FuturesPositionDTO.Latest> getFuturesPositionLatest(
            @RequestParam(value = "tradeDate", required = false) String tradeDate,
            @RequestParam("scopeType") String scopeType,
            @RequestParam(value = "exchangeCode", required = false) String exchangeCode,
            @RequestParam(value = "varietyCode", required = false) String varietyCode,
            @RequestParam("top") int top,
            @RequestParam("sort") String sort,
            @RequestParam("sortDir") String sortDir,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @GetMapping("/futures-position/instruments")
    Result<FuturesPositionDTO.Latest> getFuturesPositionInstruments(
            @RequestParam(value = "tradeDate", required = false) String tradeDate,
            @RequestParam("scopeType") String scopeType,
            @RequestParam(value = "exchangeCode", required = false) String exchangeCode,
            @RequestParam(value = "varietyCode", required = false) String varietyCode,
            @RequestParam("top") int top,
            @RequestParam("sort") String sort,
            @RequestParam("sortDir") String sortDir,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @GetMapping("/futures-position/{scopeType}/{exchangeCode}/{instrumentCode}/members")
    Result<List<FuturesPositionDTO.Member>> getFuturesPositionMembers(
            @PathVariable("scopeType") String scopeType,
            @PathVariable("exchangeCode") String exchangeCode,
            @PathVariable("instrumentCode") String instrumentCode,
            @RequestParam(value = "tradeDate", required = false) String tradeDate,
            @RequestParam(value = "rankType", required = false) String rankType,
            @RequestParam("top") int top);

    @GetMapping("/futures-position/{scopeType}/{exchangeCode}/{instrumentCode}/history")
    Result<List<FuturesPositionDTO.History>> getFuturesPositionHistory(
            @PathVariable("scopeType") String scopeType,
            @PathVariable("exchangeCode") String exchangeCode,
            @PathVariable("instrumentCode") String instrumentCode,
            @RequestParam(value = "tradeDate", required = false) String tradeDate,
            @RequestParam("top") int top,
            @RequestParam("limit") int limit);

    @GetMapping("/futures-position/coverage")
    Result<FuturesPositionDTO.Coverage> getFuturesPositionCoverage(
            @RequestParam(value = "tradeDate", required = false) String tradeDate);

    @GetMapping("/pulse/sector-snapshot/latest")
    Result<MarketPulseSectorSnapshotDTO> getLatestSectorSnapshot();

    @GetMapping("/pulse/market-overview-akshare")
    Result<MarketPulseOverviewDTO> getMarketOverviewAkshare();

    @GetMapping("/pulse/market-overview-akshare/archive/{tradingDate}")
    Result<MarketPulseOverviewDTO> getMarketOverviewArchive(@PathVariable("tradingDate") String tradingDate);

    @GetMapping("/pulse/market-overview-eltdx")
    Result<MarketPulseOverviewEltdxDTO> getMarketOverviewEltdx();

    @GetMapping("/pulse/market-overview-akshare/history")
    Result<MarketPulseOverviewHistoryDTO> getMarketOverviewPulseHistory(@RequestParam("range") String range);

    @GetMapping("/pulse/market-overview-manual-fund-flow")
    Result<MarketPulseManualFundFlowDTO> getManualFundFlow(@RequestParam("tradingDate") String tradingDate);

    @GetMapping("/pulse/sector-snapshot/history")
    Result<MarketPulseSectorSnapshotHistoryDTO> getSectorSnapshotHistory(@RequestParam("days") int days);

    @GetMapping("/pulse/index-returns")
    Result<MarketPulseIndexReturnsDTO> getIndexReturns(@RequestParam("days") int days);

    @GetMapping("/pulse/index-returns/history")
    Result<MarketPulseIndexReturnsHistoryDTO> getIndexReturnsHistory(@RequestParam("window") int window,
                                                                       @RequestParam("start") String start,
                                                                       @RequestParam("end") String end);

    @GetMapping("/pulse/index-kline/batch")
    Result<MarketPulseIndexKlineBatchDTO> getIndexKlineBatch(
            @RequestParam(value = "codes", required = false) String codes,
            @RequestParam("date") String date, @RequestParam("interval") String interval);

    @GetMapping("/pulse/style-sectors")
    Result<MarketPulseStyleSectorListDTO> getStyleSectors();

    @GetMapping("/pulse/style-sectors/{name}/constituents")
    Result<MarketPulseStyleSectorConstituentsDTO> getStyleSectorConstituents(@PathVariable("name") String name);

    @GetMapping("/pulse/industries/{code}/constituents")
    Result<MarketPulseStyleSectorConstituentsDTO> getIndustryConstituents(@PathVariable("code") String code);

    @GetMapping("/pulse/heatmap")
    Result<MarketPulseHeatmapDTO> getHeatmap(@RequestParam("kind") String kind,
                                              @RequestParam("top_n") int topN);

    @GetMapping("/pulse/limit-emotion")
    Result<Object> getPulseLimitEmotion();

    @GetMapping("/data-source/freshness")
    Result<DataSourceFreshnessDTO> getDataSourceFreshness();

    @GetMapping("/stock-information")
    Result<StockInformationDTO.FeedDTO> getStockInformation(@RequestParam("symbol") String symbol,
                                                             @RequestParam("limit") int limit);

    @GetMapping("/stock-information/items/content")
    Result<StockInformationContentDTO.ContentDTO> getStockInformationContent(
            @RequestParam("symbol") String symbol, @RequestParam("kind") String kind,
            @RequestParam("source") String source, @RequestParam("itemId") String itemId,
            @RequestParam("fetchIfMissing") boolean fetchIfMissing);

    @GetMapping("/stock-information/items/analysis")
    Result<StockInformationContentDTO.AnalysisDTO> getStockInformationAnalysis(
            @RequestParam("symbol") String symbol, @RequestParam("kind") String kind,
            @RequestParam("source") String source, @RequestParam("itemId") String itemId);

    @GetMapping("/a-stock-skill/datasets")
    Result<AStockSkillDataDTO.DatasetListDTO> getSkillDatasets();

    @GetMapping("/a-stock-skill/datasets/{datasetCode}/rows")
    Result<AStockSkillDataDTO.RowsDTO> getSkillDatasetRows(
            @PathVariable("datasetCode") String datasetCode, @RequestParam("limit") int limit,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "source", required = false) String source);
}
