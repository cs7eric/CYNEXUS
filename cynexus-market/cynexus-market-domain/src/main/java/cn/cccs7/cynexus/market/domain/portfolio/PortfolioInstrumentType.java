package cn.cccs7.cynexus.market.domain.portfolio;

public enum PortfolioInstrumentType {
    STOCK,
    ETF;

    public static PortfolioInstrumentType fromPayload(String value) {
        if (value == null || value.isBlank()) {
            // Imports created before instrument classification are treated as A-share stocks.
            return STOCK;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("instrumentType must be STOCK or ETF", exception);
        }
    }

    public String chartTargetType() {
        return this == ETF ? "etf" : "stock";
    }
}
