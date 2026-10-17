package cn.cccs7.cynexus.ai.domain.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KnowledgeRagSearchPolicyTest {

    private final KnowledgeRagSearchPolicy policy = new KnowledgeRagSearchPolicy();

    @Test
    void normalizesIdentityQueryAndTopK() {
        var request = policy.normalize(" tenant ", " user ", " question ", 99, " scope ");
        assertEquals("tenant", request.tenantKey());
        assertEquals("user", request.principalKey());
        assertEquals("question", request.query());
        assertEquals(50, request.topK());
        assertEquals("scope", request.permissionScope());
    }

    @Test
    void rejectsMissingRequiredValues() {
        assertThrows(IllegalArgumentException.class, () -> policy.normalize(null, "user", "question", null, null));
    }
}
