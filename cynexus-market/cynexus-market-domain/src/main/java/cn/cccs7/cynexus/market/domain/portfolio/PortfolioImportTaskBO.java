package cn.cccs7.cynexus.market.domain.portfolio;

import java.time.OffsetDateTime;
import lombok.Getter;

@Getter
public final class PortfolioImportTaskBO {
    private final Long id;
    private final String principalKey;
    private final String tenantKey;
    private final Long assetId;
    private final String idempotencyKey;
    private PortfolioImportStatus status;
    private long version;
    private int attemptCount;
    private OffsetDateTime queuedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime confirmedAt;
    private String lastErrorCode;
    private String lastErrorMessage;

    public PortfolioImportTaskBO(Long id, String principalKey, String tenantKey, Long assetId,
                                 String idempotencyKey, PortfolioImportStatus status,
                                 long version, int attemptCount) {
        if (id == null || principalKey == null || principalKey.isBlank()
                || tenantKey == null || tenantKey.isBlank() || assetId == null
                || idempotencyKey == null || idempotencyKey.isBlank() || status == null
                || version < 0 || attemptCount < 0) {
            throw new IllegalArgumentException("invalid portfolio import task");
        }
        this.id = id;
        this.principalKey = principalKey;
        this.tenantKey = tenantKey;
        this.assetId = assetId;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.version = version;
        this.attemptCount = attemptCount;
    }

    public static PortfolioImportTaskBO rehydrate(Long id, String principalKey, String tenantKey, Long assetId,
                                                  String idempotencyKey, PortfolioImportStatus status, long version,
                                                  int attemptCount, OffsetDateTime queuedAt, OffsetDateTime startedAt,
                                                  OffsetDateTime completedAt, OffsetDateTime confirmedAt,
                                                  String lastErrorCode, String lastErrorMessage) {
        PortfolioImportTaskBO task = new PortfolioImportTaskBO(id, principalKey, tenantKey, assetId,
                idempotencyKey, status, version, attemptCount);
        task.queuedAt = queuedAt;
        task.startedAt = startedAt;
        task.completedAt = completedAt;
        task.confirmedAt = confirmedAt;
        task.lastErrorCode = lastErrorCode;
        task.lastErrorMessage = lastErrorMessage;
        return task;
    }

    public void markQueued(OffsetDateTime occurredAt) {
        requireStatus(PortfolioImportStatus.PENDING_DISPATCH, PortfolioImportStatus.RETRY_WAITING);
        this.status = PortfolioImportStatus.QUEUED;
        this.queuedAt = requiredTime(occurredAt);
        this.version++;
    }

    public boolean claimForProcessing(OffsetDateTime startedAt) {
        if (status == PortfolioImportStatus.PROCESSING) {
            return false;
        }
        requireStatus(PortfolioImportStatus.PENDING_DISPATCH, PortfolioImportStatus.QUEUED,
                PortfolioImportStatus.RETRY_WAITING);
        this.status = PortfolioImportStatus.PROCESSING;
        this.startedAt = requiredTime(startedAt);
        this.attemptCount++;
        this.version++;
        return true;
    }

    public void markReadyForReview(OffsetDateTime completedAt) {
        requireStatus(PortfolioImportStatus.PROCESSING);
        this.status = PortfolioImportStatus.READY_FOR_REVIEW;
        this.completedAt = requiredTime(completedAt);
        this.version++;
    }

    public void markRetryWaiting(String errorCode, String errorMessage) {
        requireStatus(PortfolioImportStatus.PROCESSING);
        this.status = PortfolioImportStatus.RETRY_WAITING;
        this.lastErrorCode = requiredText(errorCode, "errorCode");
        this.lastErrorMessage = truncate(errorMessage);
        this.version++;
    }

    public void markFailed(String errorCode, String errorMessage, OffsetDateTime completedAt) {
        requireStatus(PortfolioImportStatus.PROCESSING, PortfolioImportStatus.RETRY_WAITING);
        this.status = PortfolioImportStatus.FAILED;
        this.lastErrorCode = requiredText(errorCode, "errorCode");
        this.lastErrorMessage = truncate(errorMessage);
        this.completedAt = requiredTime(completedAt);
        this.version++;
    }

    public void confirm(OffsetDateTime confirmedAt) {
        requireStatus(PortfolioImportStatus.READY_FOR_REVIEW);
        this.status = PortfolioImportStatus.CONFIRMED;
        this.confirmedAt = requiredTime(confirmedAt);
        this.version++;
    }

    public void cancel(OffsetDateTime completedAt) {
        if (status == PortfolioImportStatus.CONFIRMED || status == PortfolioImportStatus.CANCELLED) {
            throw new IllegalStateException("portfolio import task is already terminal");
        }
        this.status = PortfolioImportStatus.CANCELLED;
        this.completedAt = requiredTime(completedAt);
        this.version++;
    }

    public void retry() {
        requireStatus(PortfolioImportStatus.FAILED);
        this.status = PortfolioImportStatus.PENDING_DISPATCH;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
        this.version++;
    }

    private void requireStatus(PortfolioImportStatus... allowed) {
        for (PortfolioImportStatus value : allowed) {
            if (status == value) {
                return;
            }
        }
        throw new IllegalStateException("invalid portfolio import transition from " + status);
    }

    private static OffsetDateTime requiredTime(OffsetDateTime value) {
        if (value == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
        return value;
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
