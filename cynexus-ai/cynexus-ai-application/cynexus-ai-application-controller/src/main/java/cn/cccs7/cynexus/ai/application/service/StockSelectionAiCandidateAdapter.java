package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.stockselection.StockSelectionAiCandidatePort;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StockScreenerDTO;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Maps the existing market screener's typed internal contract into bounded AI input. */
@Component
public class StockSelectionAiCandidateAdapter implements StockSelectionAiCandidatePort {

    private final MarketResearchFeignClient marketClient;

    public StockSelectionAiCandidateAdapter(MarketResearchFeignClient marketClient) {
        this.marketClient = marketClient;
    }

    @Override
    public ScreeningSnapshot screen(Map<String, Object> criteria, int maxCandidates) {
        Result<StockScreenerDTO.Response> result = marketClient.getStockScreener(
                text(criteria, "tradeDate"), text(criteria, "universe"), bool(criteria, "excludeSt", true),
                bool(criteria, "excludeChiNext", true), bool(criteria, "excludeStar", true),
                decimal(criteria, "minPrice"), decimal(criteria, "maxPrice"), decimal(criteria, "minChangePct"), decimal(criteria, "maxChangePct"),
                decimal(criteria, "minAmount"), decimal(criteria, "minMarketCap"), decimal(criteria, "maxMarketCap"),
                text(criteria, "industry"), text(criteria, "tdxIndustryCode"), text(criteria, "thsIndustryCode"),
                text(criteria, "conceptSectorId"), text(criteria, "styleSectorId"), text(criteria, "board"),
                decimal(criteria, "minAmplitudePct"), decimal(criteria, "maxAmplitudePct"), decimal(criteria, "minReturnPct5d"), decimal(criteria, "maxReturnPct5d"),
                decimal(criteria, "minReturnPct10d"), decimal(criteria, "maxReturnPct10d"), decimal(criteria, "minReturnPct30d"), decimal(criteria, "maxReturnPct30d"),
                decimal(criteria, "minReturnPct60d"), decimal(criteria, "maxReturnPct60d"), decimal(criteria, "minReturnPct252d"), decimal(criteria, "maxReturnPct252d"),
                decimal(criteria, "minMainNet"), decimal(criteria, "minMainNet5d"), decimal(criteria, "minTurnoverPct"), decimal(criteria, "maxTurnoverPct"),
                decimal(criteria, "minVolumeRatio"), decimal(criteria, "maxVolumeRatio"), decimal(criteria, "minPe"), decimal(criteria, "maxPe"),
                text(criteria, "sort"), text(criteria, "sortDir"), 1, maxCandidates,
                integer(criteria, "volumeContractionDays"), decimal(criteria, "minBreakoutVolumeMultiple"), decimal(criteria, "maxBreakoutVolumeMultiple"), decimal(criteria, "minBullishBodyPct"),
                decimal(criteria, "minCloseAboveMa20Pct"), decimal(criteria, "minMa5AboveMa20Pct"), integer(criteria, "breakoutHighDays"),
                decimal(criteria, "maxAtrPct20"), integer(criteria, "minConsecutiveBullishDays"), decimal(criteria, "minVolumeVsMa5Multiple"),
                decimal(criteria, "maxVolumeVsMa5Multiple"), text(criteria, "candlestickPattern"));
        if (result == null || result.isFailed() || result.getData() == null) {
            String message = result == null || result.getMessage() == null ? "market screener returned no response" : result.getMessage();
            throw new IllegalStateException("market.screener failed: " + message);
        }
        StockScreenerDTO.Response response = result.getData();
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (StockScreenerDTO.Item item : response.getItems() == null ? List.<StockScreenerDTO.Item>of() : response.getItems()) {
            if (candidates.size() >= maxCandidates) break;
            candidates.add(candidate(item));
        }
        return new ScreeningSnapshot(response.getTradeDate(), response.getSource(), response.getUniverseCount(), response.getCount(),
                candidates, response.getWarnings() == null ? List.of() : List.copyOf(response.getWarnings()));
    }

    private Map<String, Object> candidate(StockScreenerDTO.Item item) {
        Map<String, Object> value = new LinkedHashMap<>();
        put(value, "code", item.getCode()); put(value, "name", item.getName()); put(value, "market", item.getMarket());
        put(value, "industry", item.getIndustry()); put(value, "board", item.getBoard()); put(value, "price", item.getPrice());
        put(value, "changePct", item.getChangePct()); put(value, "amplitudePct", item.getAmplitudePct()); put(value, "amount", item.getAmount());
        put(value, "totalMarketCap", item.getTotalMarketCap()); put(value, "floatMarketCap", item.getFloatMarketCap());
        put(value, "returnPct5d", item.getReturnPct5d()); put(value, "returnPct10d", item.getReturnPct10d()); put(value, "returnPct30d", item.getReturnPct30d());
        put(value, "returnPct60d", item.getReturnPct60d()); put(value, "returnPct252d", item.getReturnPct252d());
        put(value, "mainNet", item.getMainNet()); put(value, "mainNet5d", item.getMainNet5d()); put(value, "turnoverPct", item.getTurnoverPct());
        put(value, "volumeRatio", item.getVolumeRatio()); put(value, "pe", item.getPe()); put(value, "tdxIndustryName", item.getTdxIndustryName());
        put(value, "thsIndustryName", item.getThsIndustryName()); put(value, "priceVolumePattern", item.getPriceVolumePattern());
        if (item.getConcepts() != null) value.put("concepts", item.getConcepts().stream().map(part -> Map.of("id", part.getSectorId(), "name", part.getName())).toList());
        if (item.getStyles() != null) value.put("styles", item.getStyles().stream().map(part -> Map.of("id", part.getSectorId(), "name", part.getName())).toList());
        return value;
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) target.put(key, value);
    }

    private String text(Map<String, Object> criteria, String key) {
        Object value = criteria.get(key);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }

    private Boolean bool(Map<String, Object> criteria, String key, boolean fallback) {
        Object value = criteria.get(key);
        return value == null ? fallback : value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private BigDecimal decimal(Map<String, Object> criteria, String key) {
        Object value = criteria.get(key);
        if (value == null || String.valueOf(value).isBlank()) return null;
        try { return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value)); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException(key + " must be a number", exception); }
    }

    private Integer integer(Map<String, Object> criteria, String key) {
        Object value = criteria.get(key);
        if (value == null || String.valueOf(value).isBlank()) return null;
        if (value instanceof Number number) return number.intValue();
        try { return Integer.valueOf(String.valueOf(value)); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException(key + " must be an integer", exception); }
    }
}
