package cn.cccs7.cynexus.market.domain.portfolio;

public enum PortfolioImportProcessEventType {
    TASK_CREATED,
    OUTBOX_CREATED,
    PROCESSING_STARTED,
    AI_REQUEST_STARTED,
    AI_REQUEST_SUCCEEDED,
    AI_REQUEST_FAILED,
    RETRY_SCHEDULED,
    CANDIDATES_SAVED,
    CANDIDATE_EDITED,
    TASK_READY_FOR_REVIEW,
    TASK_CONFIRMED,
    TASK_FAILED
}
