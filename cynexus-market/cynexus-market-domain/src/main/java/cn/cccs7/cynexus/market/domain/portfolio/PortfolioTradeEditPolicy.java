package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class PortfolioTradeEditPolicy {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private PortfolioTradeEditPolicy() {
    }

    public static PortfolioTradeBO normalize(Long id, String symbol, String securityName, String instrumentType,
                                              String side, String tradedAt, String quantity, String price,
                                              String totalFee, TradeStatus status) {
        String normalizedSymbol = required(symbol, "symbol");
        if (!normalizedSymbol.matches("\\d{6}")) {
            throw new IllegalArgumentException("symbol must be a 6 digit mainland stock or ETF code");
        }
        PortfolioInstrumentType normalizedInstrument = PortfolioInstrumentType.fromPayload(instrumentType);
        TradeSide normalizedSide;
        try {
            normalizedSide = TradeSide.valueOf(required(side, "side").toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("side must be BUY or SELL", exception);
        }
        BigDecimal normalizedQuantity = positive(quantity, "quantity");
        BigDecimal normalizedPrice = positive(price, "price");
        BigDecimal normalizedFee = nonNegative(totalFee, "totalFee");
        OffsetDateTime normalizedTime = parseShanghaiTime(tradedAt);
        return new PortfolioTradeBO(id, normalizedSymbol, blankToNull(securityName), normalizedInstrument,
                normalizedSide, normalizedTime, normalizedQuantity, normalizedPrice, normalizedFee,
                status == null ? TradeStatus.ACTIVE : status);
    }

    private static OffsetDateTime parseShanghaiTime(String value) {
        String normalized = required(value, "tradedAt");
        try {
            return OffsetDateTime.parse(normalized);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(normalized).atZone(SHANGHAI).toOffsetDateTime();
            } catch (Exception localDateTimeIgnored) {
                try {
                    return LocalDate.parse(normalized).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
                } catch (Exception dateIgnored) {
                    throw new IllegalArgumentException("tradedAt must be an ISO date or timestamp", dateIgnored);
                }
            }
        }
    }

    private static BigDecimal positive(String value, String field) {
        BigDecimal parsed = decimal(value, field);
        if (parsed.signum() <= 0) throw new IllegalArgumentException(field + " must be greater than zero");
        return parsed;
    }

    private static BigDecimal nonNegative(String value, String field) {
        BigDecimal parsed = decimal(value == null || value.isBlank() ? "0" : value, field);
        if (parsed.signum() < 0) throw new IllegalArgumentException(field + " must not be negative");
        return parsed;
    }

    private static BigDecimal decimal(String value, String field) {
        try {
            return new BigDecimal(required(value, field));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be numeric", exception);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
