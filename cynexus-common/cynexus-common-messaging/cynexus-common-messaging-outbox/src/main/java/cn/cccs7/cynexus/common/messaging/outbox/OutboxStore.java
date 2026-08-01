package cn.cccs7.cynexus.common.messaging.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface OutboxStore {

    List<OutboxRecord> claimReady(Instant now, int limit, String leaseOwner, Duration leaseDuration);

    void markPublished(String recordId, String leaseOwner, Instant publishedAt);

    void markRetry(String recordId, String leaseOwner, Instant availableAt, String errorCode, String errorMessage);

    void markFailed(String recordId, String leaseOwner, String errorCode, String errorMessage);
}
