package cn.cccs7.cynexus.ai.domain.futuresposition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FuturesPositionAiDomainPolicy {

    private static final Set<String> ALLOWED_VARIETIES = Set.of("IF", "IH", "IM", "IC");
    private static final Pattern VARIETY_PREFIX = Pattern.compile("^([A-Z]{2})");
    private static final int MAX_HISTORY = 1100;
    private static final int MAX_MEMBERS = 90;

    public String requireAllowedVariety(String varietyCode, String instrumentCode) {
        String normalized = normalize(varietyCode);
        if (normalized.isBlank()) {
            Matcher matcher = VARIETY_PREFIX.matcher(normalize(instrumentCode));
            normalized = matcher.find() ? matcher.group(1) : "";
        }
        if (!ALLOWED_VARIETIES.contains(normalized)) {
            throw new IllegalArgumentException("AI 结构分析仅支持 IF、IH、IM、IC");
        }
        return normalized;
    }

    public Map<String, Object> buildInput(FuturesPositionAiDomainService.AnalysisCommand command,
                                          String varietyCode) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("target", Map.of(
                "variety_code", varietyCode,
                "instrument_code", value(command.instrumentCode()),
                "instrument_name", value(command.instrumentName()),
                "exchange_code", value(command.exchangeCode()),
                "trade_date", value(command.tradeDate()),
                "top", command.top() == null ? 20 : command.top()));
        input.put("current", command.current() == null ? Map.of() : command.current());
        input.put("history", tail(command.history(), MAX_HISTORY));
        input.put("members", tail(command.members(), MAX_MEMBERS));
        input.put("analysis_scope", List.of("当前多空结构", "历史分位与持续性", "会员增减仓证据"));
        return input;
    }

    private List<Map<String, Object>> tail(List<Map<String, Object>> values, int max) {
        if (values == null || values.isEmpty()) return List.of();
        int start = Math.max(0, values.size() - max);
        return List.copyOf(values.subList(start, values.size()));
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
