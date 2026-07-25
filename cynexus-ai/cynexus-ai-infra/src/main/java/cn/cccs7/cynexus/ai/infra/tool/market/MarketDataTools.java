package cn.cccs7.cynexus.ai.infra.tool.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.feign.MarketFeignClient;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.SectorRankDTO;
import cn.cccs7.cynexus.market.api.dto.StockChartDTO;
import cn.cccs7.cynexus.market.api.dto.StockDailyFeatureDTO;
import cn.cccs7.cynexus.market.api.dto.StockIntradayFeatureDTO;
import feign.FeignException;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class MarketDataTools implements MarketDataGateway {

    private final MarketFeignClient marketFeignClient;

    public MarketDataTools(MarketFeignClient marketFeignClient) {
        this.marketFeignClient = marketFeignClient;
    }

    @Override
    public MarketOverviewDTO getMarketOverview(String tradeDate) {
        return unwrap("market.overview", () -> marketFeignClient.getMarketOverview(tradeDate));
    }

    @Override
    public MarketPulseOverviewHistoryDTO getMarketOverviewHistory(String start, String end) {
        return unwrap("market.overview_history", () -> marketFeignClient.getMarketOverviewHistory(start, end));
    }

    @Override
    public SectorRankDTO getSectorRank(String tradeDate, Integer topN) {
        return unwrap("market.sector_rank", () -> marketFeignClient.getSectorRank(tradeDate, topN == null ? 10 : topN));
    }

    @Override
    public StockDailyFeatureDTO getStockDailyFeature(String symbol, String tradeDate) {
        return unwrap("market.stock_daily_feature", () -> marketFeignClient.getStockDailyFeature(symbol, tradeDate));
    }

    @Override
    public StockIntradayFeatureDTO getStockIntradayFeature(String symbol, String tradeDate) {
        return unwrap("market.stock_intraday_feature", () -> marketFeignClient.getStockIntradayFeature(symbol, tradeDate));
    }

    @Override
    public StockChartDTO.KlineResponseDTO getStockChartKlines(
            String targetType,
            String symbol,
            String period,
            String adjust) {
        return unwrap("market.stock_chart_klines",
                () -> marketFeignClient.getStockChartKlines(targetType, symbol, period, adjust));
    }

    @Override
    public Map<String, Object> getMarketInformationFeed(Integer page, Integer pageSize, String impactIndustryCode,
                                                         String analysisStatus) {
        return unwrap("market.information_feed", () -> marketFeignClient.getMarketInformationFeed(
                page == null ? 1 : page,
                pageSize == null ? 20 : pageSize,
                impactIndustryCode,
                analysisStatus));
    }

    private static <T> T unwrap(String tool, FeignCall<T> call) {
        try {
            Result<T> result = call.get();
            if (result == null) {
                throw new IllegalStateException(tool + " returned an empty response from market service");
            }
            if (result.isFailed()) {
                String message = result.getMessage() == null || result.getMessage().isBlank()
                        ? "market service call failed"
                        : result.getMessage();
                throw new IllegalStateException(tool + " failed: " + message);
            }
            return result.getData();
        } catch (FeignException exception) {
            String body = exception.contentUTF8();
            String detail = body == null || body.isBlank() ? exception.getMessage() : body;
            throw new IllegalStateException(tool + " HTTP " + exception.status() + ": " + detail, exception);
        }
    }

    @FunctionalInterface
    private interface FeignCall<T> {
        Result<T> get();
    }
}
