package cn.cccs7.cynexus.common.messaging.outbox;

import java.time.Instant;

public interface InboxStore {

    boolean tryStart(String consumerName, String messageId, Instant startedAt);

    void markProcessed(String consumerName, String messageId, Instant processedAt);

    void markFailed(String consumerName, String messageId, String errorCode, String errorMessage, Instant failedAt);
}
