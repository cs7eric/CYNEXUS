package cn.cccs7.cynexus.market.domain.portfolio;

public record PortfolioImportFailureDecision(boolean retry, String errorCode, String errorMessage) {
    public PortfolioImportFailureDecision {
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode must not be blank");
        }
    }
}
