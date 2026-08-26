package cn.cccs7.cynexus.market.domain.stockselection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class StockSelectionStrategyRunBO {

    private StockSelectionStrategyRunBO() {
    }

    public record Scope(String tenantId, String ownerId) {
        public Scope {
            if (tenantId == null || tenantId.isBlank() || ownerId == null || ownerId.isBlank()) {
                throw new IllegalArgumentException("stock selection scope is required");
            }
        }
    }

    public record Run(long id,
                      Scope scope,
                      String strategyCode,
                      long strategyVersionId,
                      int strategyVersionNo,
                      LocalDate requestedTradeDate,
                      LocalDate actualTradeDate,
                      String status,
                      Map<String, Object> parameterSnapshot,
                      String source,
                      int universeCount,
                      int matchedCount,
                      List<String> warnings,
                      OffsetDateTime startedAt,
                      OffsetDateTime completedAt,
                      List<Candidate> candidates) {
    }

    public record Candidate(long id,
                            String stockCode,
                            String stockName,
                            int rank,
                            BigDecimal score,
                            String reasonSummary,
                            Map<String, Object> snapshot,
                            List<Evidence> evidence) {
    }

    public record Evidence(long id,
                           String code,
                           String label,
                           String operator,
                           Object expectedValue,
                           Object actualValue,
                           boolean matched) {
    }

    /** Minimal immutable-run fields used for fast, owner-scoped health assessment. */
    public record HealthInput(String strategyCode,
                              int strategyVersionNo,
                              LocalDate requestedTradeDate,
                              LocalDate actualTradeDate,
                              String status,
                              int universeCount,
                              int matchedCount,
                              String source,
                              List<String> warnings) {
    }
}
