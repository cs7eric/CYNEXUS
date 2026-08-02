package cn.cccs7.cynexus.market.domain.portfolio;

/** A sanitized, classified failure returned by the portfolio vision gateway. */
public final class PortfolioTradeVisionException extends RuntimeException {
    private final String failureCode;
    private final boolean retryable;

    public PortfolioTradeVisionException(String failureCode, String message, boolean retryable) {
        super(message);
        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }
        this.failureCode = failureCode;
        this.retryable = retryable;
    }

    public String failureCode() {
        return failureCode;
    }

    public boolean retryable() {
        return retryable;
    }
}
