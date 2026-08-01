package cn.cccs7.cynexus.common.messaging.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageEnvelopeTest {

    @Test
    void copiesHeadersToKeepEnvelopeImmutable() {
        Map<String, String> source = new HashMap<>();
        source.put("tenant", "demo");

        MessageEnvelope envelope = new MessageEnvelope(
                "message-1", MessageType.EVENT, "portfolio.imported", 1,
                "cynexus-market", "portfolio-import", "import-1", Instant.now(),
                "trace-1", "correlation-1", null, "{}", source
        );

        source.put("tenant", "changed");

        assertEquals("demo", envelope.headers().get("tenant"));
        assertThrows(UnsupportedOperationException.class,
                () -> envelope.headers().put("new", "value"));
    }

    @Test
    void rejectsInvalidEnvelope() {
        assertThrows(IllegalArgumentException.class, () -> new MessageEnvelope(
                "", MessageType.EVENT, "portfolio.imported", 1,
                "cynexus-market", "portfolio-import", "import-1", Instant.now(),
                null, null, null, "{}", Map.of()
        ));
    }
}
