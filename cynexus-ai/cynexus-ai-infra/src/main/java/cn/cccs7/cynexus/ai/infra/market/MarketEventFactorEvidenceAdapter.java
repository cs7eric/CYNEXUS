package cn.cccs7.cynexus.ai.infra.market;

import cn.cccs7.cynexus.ai.domain.marketfactor.MarketEventFactorEvidencePort;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketEventFactorObservatoryDTO;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Converts the Market module's typed observatory response into a reference-addressable model input. */
@Component
public class MarketEventFactorEvidenceAdapter implements MarketEventFactorEvidencePort {
    private final MarketResearchFeignClient marketClient;

    public MarketEventFactorEvidenceAdapter(MarketResearchFeignClient marketClient) {
        this.marketClient = marketClient;
    }

    @Override
    public EvidenceSnapshot load(String tradeDate, String cohort, BigDecimal minLargeMovePct, int maxEvents, String symbol) {
        Result<MarketEventFactorObservatoryDTO> result = marketClient.getMarketEventFactorObservatory(
                tradeDate, cohort, minLargeMovePct, maxEvents, symbol);
        if (result == null || result.isFailed() || result.getData() == null || !Boolean.TRUE.equals(result.getData().getOk())) {
            String message = result == null || result.getMessage() == null ? "market factor observatory returned no response" : result.getMessage();
            throw new IllegalStateException("market.factor-observatory failed: " + message);
        }
        MarketEventFactorObservatoryDTO response = result.getData();
        LinkedHashMap<String, String> refs = new LinkedHashMap<>();
        addFamilies(refs, "market", response.getMarketFactors());
        List<String> eventCodes = new ArrayList<>();
        for (MarketEventFactorObservatoryDTO.Event event : safeEvents(response.getEvents())) {
            if (event.getCode() == null || event.getCode().isBlank()) continue;
            eventCodes.add(event.getCode());
            addFamilies(refs, "event:" + event.getCode(), event.getFactors());
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("trade_date", response.getTradeDate() == null ? null : response.getTradeDate().toString());
        input.put("cohort", response.getCohort());
        input.put("population_count", response.getPopulationCount());
        input.put("sampled_count", response.getSampledCount());
        input.put("event_codes", List.copyOf(eventCodes));
        input.put("evidence_refs", refs);
        input.put("warnings", response.getWarnings() == null ? List.of() : List.copyOf(response.getWarnings()));
        return new EvidenceSnapshot(response.getRequestedTradeDate() == null ? null : response.getRequestedTradeDate().toString(),
                response.getTradeDate() == null ? null : response.getTradeDate().toString(), response.getCohort(), response.getMinLargeMovePct(),
                value(response.getPopulationCount()), value(response.getSampledCount()), "market.factor-observatory", Map.copyOf(refs), Map.copyOf(input),
                response.getWarnings() == null ? List.of() : List.copyOf(response.getWarnings()), quality(response));
    }

    private void addFamilies(Map<String, String> refs, String prefix, List<MarketEventFactorObservatoryDTO.FactorFamily> families) {
        for (MarketEventFactorObservatoryDTO.FactorFamily family : families == null ? List.<MarketEventFactorObservatoryDTO.FactorFamily>of() : families) {
            if (family == null || family.getCode() == null) continue;
            for (MarketEventFactorObservatoryDTO.Metric metric : family.getMetrics() == null ? List.<MarketEventFactorObservatoryDTO.Metric>of() : family.getMetrics()) {
                if (metric == null || metric.getKey() == null || metric.getValue() == null) continue;
                String reference = prefix + ":" + family.getCode() + "." + metric.getKey();
                String asOf = family.getAsOfDate() == null ? "日期不可用" : family.getAsOfDate().toString();
                refs.put(reference, family.getLabel() + " / " + metric.getLabel() + " = " + metric.getValue()
                        + (metric.getUnit() == null ? "" : metric.getUnit()) + "（" + asOf + "，" + family.getStatus() + "）");
            }
        }
    }

    private String quality(MarketEventFactorObservatoryDTO response) {
        boolean partial = response.getWarnings() != null && !response.getWarnings().isEmpty();
        List<MarketEventFactorObservatoryDTO.FactorFamily> families = new ArrayList<>();
        if (response.getMarketFactors() != null) families.addAll(response.getMarketFactors());
        for (MarketEventFactorObservatoryDTO.Event event : safeEvents(response.getEvents())) if (event.getFactors() != null) families.addAll(event.getFactors());
        for (MarketEventFactorObservatoryDTO.FactorFamily family : families) {
            if (family != null && !"AVAILABLE".equalsIgnoreCase(family.getStatus()) && !"READY".equalsIgnoreCase(family.getStatus())) return "PARTIAL";
        }
        return partial ? "PARTIAL" : "READY";
    }

    private List<MarketEventFactorObservatoryDTO.Event> safeEvents(List<MarketEventFactorObservatoryDTO.Event> events) {
        return events == null ? List.of() : events;
    }

    private int value(Integer item) { return item == null ? 0 : item; }
}
