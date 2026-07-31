package cn.cccs7.cynexus.market.domain.policy;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class FuturesPositionDomainPolicy {
    public static final int DEFAULT_PAGE_SIZE = 30;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_TOP = 10;
    private static final List<String> EXCHANGES = List.of("CFFEX", "SHFE", "CZCE", "DCE", "GFEX", "INE");
    private static final List<Integer> TOPS = List.of(5, 10, 15, 20);
    private static final List<String> SORTS = List.of("volume", "longPosition", "shortPosition", "netPosition", "ratio", "instrumentCode");

    public Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.parse(value.trim()));
    }

    public String scope(String value) {
        String normalized = value == null || value.isBlank() ? "CONTRACT" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("CONTRACT") && !normalized.equals("VARIETY")) {
            throw new IllegalArgumentException("scopeType must be CONTRACT or VARIETY");
        }
        return normalized;
    }

    public String exchange(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!EXCHANGES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported futures exchange: " + normalized);
        }
        return normalized;
    }

    public String textCode(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    public int top(int value) {
        return TOPS.contains(value) ? value : DEFAULT_TOP;
    }

    public String sort(String value) {
        if (value == null || value.isBlank()) {
            return "netPosition";
        }
        return SORTS.contains(value) ? value : "netPosition";
    }

    public String sortDir(String value) {
        return "asc".equalsIgnoreCase(value) ? "asc" : "desc";
    }

    public Page page(Integer number, Integer size) {
        int safeNumber = number == null || number < 1 ? 1 : number;
        int safeSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return new Page(safeNumber, safeSize, (safeNumber - 1) * safeSize);
    }

    public int limit(int value, int defaultValue) {
        return value < 1 ? defaultValue : Math.min(value, 500);
    }

    public record Page(int number, int size, int offset) {
    }
}
