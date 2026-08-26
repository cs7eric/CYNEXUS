package cn.cccs7.cynexus.ai.domain.stockselection;

import java.util.List;
import java.util.Map;

/** Trusted, bounded market screener snapshot used as the only factual AI input. */
public interface StockSelectionAiCandidatePort {

    ScreeningSnapshot screen(Map<String, Object> criteria, int maxCandidates);

    record ScreeningSnapshot(String tradeDate,
                             String source,
                             int universeCount,
                             int matchedCount,
                             List<Map<String, Object>> candidates,
                             List<String> warnings) {
    }
}
