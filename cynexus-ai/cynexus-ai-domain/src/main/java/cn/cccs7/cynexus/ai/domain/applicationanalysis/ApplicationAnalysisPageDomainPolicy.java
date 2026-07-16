package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import java.time.LocalDate;

public final class ApplicationAnalysisPageDomainPolicy {

    public String normalizeTargetKey(String value) {
        return defaultString(value, "").toLowerCase();
    }

    public String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public String buildTargetKey(String targetType, String symbol) {
        return normalizeTargetKey(defaultString(targetType, "stock") + "-" + defaultString(symbol, ""));
    }

    public LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }
}
