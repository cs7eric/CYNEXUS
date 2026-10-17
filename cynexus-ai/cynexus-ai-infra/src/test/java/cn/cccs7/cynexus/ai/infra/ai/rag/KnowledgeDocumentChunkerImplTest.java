package cn.cccs7.cynexus.ai.infra.ai.rag;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentChunkerImplTest {
    @Test void chunksAndPreservesAllText() {
        var chunks = new KnowledgeDocumentChunkerImpl().chunk("one\n\ntwo\n\nthree", 128, 2);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().allMatch(value -> value.length() <= 128));
        assertTrue(String.join(" ", chunks).contains("one"));
        assertTrue(String.join(" ", chunks).contains("three"));
    }
}
