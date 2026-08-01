package cn.cccs7.cynexus.common.messaging.outbox;

import cn.cccs7.cynexus.common.messaging.api.MessageDestination;
import cn.cccs7.cynexus.common.messaging.api.MessageEnvelope;
import cn.cccs7.cynexus.common.messaging.api.MessagePublishReceipt;
import cn.cccs7.cynexus.common.messaging.api.MessageTransport;
import cn.cccs7.cynexus.common.messaging.api.MessageTransportException;
import cn.cccs7.cynexus.common.messaging.api.MessageType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxRelayTest {

    @Test
    void marksPublishedRecordsAfterSuccessfulTransport() {
        RecordingStore store = new RecordingStore();
        OutboxRelay relay = new OutboxRelay(store, (destination, envelope) ->
                new MessagePublishReceipt(envelope.messageId(), Instant.now()),
                10, Duration.ofMinutes(1), 3);

        assertEquals(1, relay.relayOnce(Instant.parse("2026-08-01T00:00:00Z"), "worker-1"));
        assertEquals(List.of("published:record-1"), store.actions);
    }

    @Test
    void schedulesRetryAndEventuallyMarksFailed() {
        RecordingStore store = new RecordingStore();
        MessageTransport failingTransport = (destination, envelope) -> {
            throw new MessageTransportException("broker unavailable");
        };
        OutboxRelay relay = new OutboxRelay(store, failingTransport, 10, Duration.ofMinutes(1), 2);

        relay.relayOnce(Instant.parse("2026-08-01T00:00:00Z"), "worker-1");
        assertEquals(List.of("retry:record-1"), store.actions);

        store.record = new OutboxRecord("record-1", destination(), envelope(),
                Instant.parse("2026-08-01T00:01:00Z"), 1);
        relay.relayOnce(Instant.parse("2026-08-01T00:01:00Z"), "worker-1");
        assertEquals(List.of("retry:record-1", "failed:record-1"), store.actions);
    }

    private static MessageDestination destination() {
        return new MessageDestination("portfolio.events", "import.completed");
    }

    private static MessageEnvelope envelope() {
        return new MessageEnvelope(
                "message-1", MessageType.EVENT, "portfolio.import.completed", 1,
                "cynexus-market", "portfolio-import", "import-1", Instant.now(),
                null, null, null, "{}", Map.of()
        );
    }

    private static final class RecordingStore implements OutboxStore {
        private final List<String> actions = new ArrayList<>();
        private OutboxRecord record = new OutboxRecord("record-1", destination(), envelope(), Instant.now(), 0);

        @Override
        public List<OutboxRecord> claimReady(Instant now, int limit, String leaseOwner, Duration leaseDuration) {
            return List.of(record);
        }

        @Override
        public void markPublished(String recordId, String leaseOwner, Instant publishedAt) {
            actions.add("published:" + recordId);
        }

        @Override
        public void markRetry(String recordId, String leaseOwner, Instant availableAt, String errorCode, String errorMessage) {
            actions.add("retry:" + recordId);
        }

        @Override
        public void markFailed(String recordId, String leaseOwner, String errorCode, String errorMessage) {
            actions.add("failed:" + recordId);
        }
    }
}
