package cn.cccs7.cynexus.common.messaging.outbox;

import cn.cccs7.cynexus.common.messaging.api.MessageTransport;
import cn.cccs7.cynexus.common.messaging.api.MessageTransportException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class OutboxRelay {

    private final OutboxStore store;
    private final MessageTransport transport;
    private final int batchSize;
    private final Duration leaseDuration;
    private final int maxAttempts;

    public OutboxRelay(
            OutboxStore store,
            MessageTransport transport,
            int batchSize,
            Duration leaseDuration,
            int maxAttempts
    ) {
        if (store == null || transport == null || leaseDuration == null
                || batchSize < 1 || leaseDuration.isNegative() || leaseDuration.isZero() || maxAttempts < 1) {
            throw new IllegalArgumentException("Invalid outbox relay configuration");
        }
        this.store = store;
        this.transport = transport;
        this.batchSize = batchSize;
        this.leaseDuration = leaseDuration;
        this.maxAttempts = maxAttempts;
    }

    public int relayOnce(Instant now, String leaseOwner) {
        if (now == null || leaseOwner == null || leaseOwner.isBlank()) {
            throw new IllegalArgumentException("now and leaseOwner are required");
        }
        List<OutboxRecord> records = store.claimReady(now, batchSize, leaseOwner, leaseDuration);
        for (OutboxRecord record : records) {
            publish(record, now, leaseOwner);
        }
        return records.size();
    }

    private void publish(OutboxRecord record, Instant now, String leaseOwner) {
        try {
            transport.publish(record.destination(), record.envelope());
            store.markPublished(record.id(), leaseOwner, now);
        } catch (MessageTransportException exception) {
            int nextAttempt = record.attemptCount() + 1;
            String code = "MESSAGE_PUBLISH_FAILED";
            String message = truncate(exception.getMessage());
            if (nextAttempt >= maxAttempts) {
                store.markFailed(record.id(), leaseOwner, code, message);
            } else {
                store.markRetry(record.id(), leaseOwner, now.plus(backoff(nextAttempt)), code, message);
            }
        }
    }

    private static Duration backoff(int attempt) {
        long seconds = Math.min(600L, 30L * (1L << Math.min(attempt - 1, 4)));
        return Duration.ofSeconds(seconds);
    }

    private static String truncate(String message) {
        if (message == null) {
            return "unknown transport error";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
