package cn.cccs7.cynexus.market.domain.portfolio;

public enum PortfolioImportStatus {
    PENDING_DISPATCH,
    QUEUED,
    PROCESSING,
    RETRY_WAITING,
    READY_FOR_REVIEW,
    CONFIRMED,
    FAILED,
    CANCELLED
}
