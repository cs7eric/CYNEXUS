package cn.cccs7.cynexus.market.domain.margin;

import cn.cccs7.cynexus.market.domain.entity.MarginAccountStatsBO;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class MarginTradingDomainPolicy {

    public static final int DEFAULT_TOP_STOCK_LIMIT = 20;
    public static final int DEFAULT_STOCK_LIMIT = 50;
    public static final int DEFAULT_RANKING_PAGE_SIZE = 50;
    public static final int MAX_STOCK_LIMIT = 500;
    public static final int DEFAULT_HISTORY_DAYS = 120;

    public Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim();
        if (normalized.length() == 8 && !normalized.contains("-")) {
            normalized = normalized.substring(0, 4) + "-" + normalized.substring(4, 6) + "-" + normalized.substring(6, 8);
        }
        try {
            return Optional.of(LocalDate.parse(normalized));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("trade_date must be yyyy-MM-dd", ex);
        }
    }

    public int clampLimit(int requested, int fallback) {
        int value = requested <= 0 ? fallback : requested;
        return Math.max(1, Math.min(value, MAX_STOCK_LIMIT));
    }

    public String normalizeExchange(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    public String normalizeIndustryCode(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String normalizeStockCode(String value) {
        return value == null ? "" : value.trim().replaceAll("^(?i)(SH|SZ|BJ)", "");
    }

    public String normalizeStockSort(String value) {
        if (value == null || value.isBlank()) {
            return "margin_balance";
        }
        return switch (value.trim()) {
            case "stock_code", "stock_name", "exchange", "financing_balance", "financing_buy_amount",
                    "financing_repay_amount", "securities_lending_balance", "securities_lending_balance_amount",
                    "margin_balance" -> value.trim();
            default -> "margin_balance";
        };
    }

    public String normalizeIndustrySort(String value) {
        if (value == null || value.isBlank()) {
            return "margin_balance";
        }
        return switch (value.trim()) {
            case "industry_code", "industry_name", "tdx_code", "tdx_name", "constituent_count",
                    "matched_stock_count", "financing_balance", "financing_buy_amount", "financing_repay_amount",
                    "securities_lending_balance", "securities_lending_balance_amount", "margin_balance" -> value.trim();
            default -> "margin_balance";
        };
    }

    public String normalizeSortDirection(String value) {
        return value != null && "asc".equalsIgnoreCase(value.trim()) ? "asc" : "desc";
    }

    public ServingMetadata servingMetadata(List<MarginMarketSummaryBO> summaries,
                                           MarginAccountStatsBO account,
                                           List<MarginStockDetailBO> stocks) {
        String source = summaries == null ? null : summaries.stream()
                .map(MarginMarketSummaryBO::getSource)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(account == null ? null : account.getSource());
        Stream<OffsetDateTime> timestamps = Stream.concat(
                Stream.concat(
                        summaries == null ? Stream.empty() : summaries.stream().map(MarginMarketSummaryBO::getUpdatedAt),
                        stocks == null ? Stream.empty() : stocks.stream().map(MarginStockDetailBO::getUpdatedAt)),
                Stream.of(account == null ? null : account.getUpdatedAt())
        );
        OffsetDateTime updatedAt = timestamps.filter(value -> value != null).max(Comparator.naturalOrder()).orElse(null);
        return new ServingMetadata(source, updatedAt);
    }

    public record ServingMetadata(String source, OffsetDateTime updatedAt) {
    }
}
