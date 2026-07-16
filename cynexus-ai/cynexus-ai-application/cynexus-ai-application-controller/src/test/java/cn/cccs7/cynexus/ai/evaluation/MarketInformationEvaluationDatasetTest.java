package cn.cccs7.cynexus.ai.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketInformationEvaluationDatasetTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void contains60StableCasesWith20GoldAndExactEvidence() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/market-information/evaluation-60.json")) {
            assertNotNull(stream);
            JsonNode cases = objectMapper.readTree(stream);
            assertEquals(60, cases.size());
            assertEquals(20, java.util.stream.StreamSupport.stream(cases.spliterator(), false)
                    .filter(item -> item.path("gold").asBoolean()).count());
            Set<String> identities = new HashSet<>();
            for (JsonNode item : cases) {
                assertTrue(identities.add(item.path("source").asText() + ":" + item.path("sourceRecordId").asText()));
                assertFalse(item.path("title").asText().isBlank());
                assertTrue(item.path("content").asText().contains(item.path("expected").path("evidenceQuote").asText()));
                assertTrue(Set.of("policy", "macro", "industry", "company", "regulation", "market")
                        .contains(item.path("expected").path("eventType").asText()));
            }
        }
    }
}
