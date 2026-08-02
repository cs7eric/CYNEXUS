package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class PortfolioCandidateEditPolicy {
    private PortfolioCandidateEditPolicy() {
    }

    public static ObjectNode normalize(ObjectMapper mapper, String symbol, String securityName, String instrumentType, String side,
                                       String tradedAt, String quantity, String price, String totalFee,
                                       String externalTradeNo) {
        ObjectNode result = mapper.createObjectNode();
        putRequired(result, "symbol", symbol == null ? null : symbol.trim());
        if (!result.path("symbol").asText().matches("\\d{6}")) {
            throw new IllegalArgumentException("symbol must be a 6 digit mainland stock or ETF code");
        }
        result.put("instrumentType", PortfolioInstrumentType.fromPayload(instrumentType).name());
        String normalizedSide = required(side, "side").toUpperCase();
        if (!"BUY".equals(normalizedSide) && !"SELL".equals(normalizedSide)) {
            throw new IllegalArgumentException("side must be BUY or SELL");
        }
        result.put("side", normalizedSide);
        String normalizedTime = required(tradedAt, "tradedAt");
        try {
            normalizedTime = OffsetDateTime.parse(normalizedTime).toString();
        } catch (Exception ignored) {
            try {
                normalizedTime = LocalDate.parse(normalizedTime).atStartOfDay().atOffset(ZoneOffset.UTC).toString();
            } catch (Exception exception) {
                throw new IllegalArgumentException("tradedAt must be an ISO date or timestamp", exception);
            }
        }
        result.put("tradedAt", normalizedTime);
        result.put("quantity", positive(quantity, "quantity").toPlainString());
        result.put("price", positive(price, "price").toPlainString());
        result.put("fee", nonNegative(totalFee == null || totalFee.isBlank() ? "0" : totalFee, "totalFee").toPlainString());
        putNullable(result, "securityName", securityName);
        putNullable(result, "externalTradeNo", externalTradeNo);
        return result;
    }

    private static void putRequired(ObjectNode node, String field, String value) {
        node.put(field, required(value, field));
    }

    private static void putNullable(ObjectNode node, String field, String value) {
        if (value == null || value.isBlank()) node.putNull(field); else node.put(field, value.trim());
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static BigDecimal positive(String value, String field) {
        BigDecimal parsed = decimal(value, field);
        if (parsed.signum() <= 0) throw new IllegalArgumentException(field + " must be greater than zero");
        return parsed;
    }

    private static BigDecimal nonNegative(String value, String field) {
        BigDecimal parsed = decimal(value, field);
        if (parsed.signum() < 0) throw new IllegalArgumentException(field + " must not be negative");
        return parsed;
    }

    private static BigDecimal decimal(String value, String field) {
        try { return new BigDecimal(required(value, field)); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException(field + " must be numeric", exception); }
    }
}
