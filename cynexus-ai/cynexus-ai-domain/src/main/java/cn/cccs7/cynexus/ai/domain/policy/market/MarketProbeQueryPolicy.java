package cn.cccs7.cynexus.ai.domain.policy.market;

/** Domain normalization rules for AI-facing market probe queries. */
public final class MarketProbeQueryPolicy {

    public static final int DEFAULT_TOP_N = 10;
    public static final int MIN_TOP_N = 1;
    public static final int MAX_TOP_N = 100;

    public String requireTradeDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("tradeDate is required");
        }
        return value.trim();
    }

    public String requireSymbol(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        return value.trim();
    }

    public int normalizeTopN(Integer value) {
        if (value == null) {
            return DEFAULT_TOP_N;
        }
        return Math.max(MIN_TOP_N, Math.min(value, MAX_TOP_N));
    }
}
