package cn.cccs7.cynexus.market.domain.portfolio;

/** Decides whether a failed parse attempt may safely be retried. */
public final class PortfolioImportFailurePolicy {
    public static final int MAX_RETRY_COUNT = 3;

    private PortfolioImportFailurePolicy() {
    }

    public static PortfolioImportFailureDecision decide(Exception exception, int retryCount) {
        String message = messageOf(exception);
        if (exception instanceof PortfolioTradeVisionException visionFailure) {
            if (!visionFailure.retryable()) {
                return new PortfolioImportFailureDecision(false, visionFailure.failureCode(), message);
            }
            if (retryCount >= MAX_RETRY_COUNT) {
                return new PortfolioImportFailureDecision(false, visionFailure.failureCode(), message);
            }
        } else if (retryCount >= MAX_RETRY_COUNT) {
            return new PortfolioImportFailureDecision(false, "PORTFOLIO_PARSE_FAILED", message);
        }
        return new PortfolioImportFailureDecision(true, "PORTFOLIO_PARSE_RETRY", message);
    }

    private static String messageOf(Exception exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "Portfolio trade screenshot parsing failed.";
        }
        String message = exception.getMessage().trim();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
