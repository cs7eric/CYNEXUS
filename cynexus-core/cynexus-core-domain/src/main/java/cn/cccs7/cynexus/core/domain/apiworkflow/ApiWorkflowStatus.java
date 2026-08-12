package cn.cccs7.cynexus.core.domain.apiworkflow;

public enum ApiWorkflowStatus {
    QUEUED,
    RUNNING,
    WAITING_REVIEW,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
