package cn.cccs7.cynexus.ai.infra.ai.rag;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import cn.cccs7.cynexus.ai.domain.port.KnowledgeRagPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import java.util.List;
import java.util.Map;

class SpringAiKnowledgeRagAdapterTest {
    @Test void missingIdentityDoesNotQueryVectorStore() {
        VectorStore store = mock(VectorStore.class);
        var adapter = new SpringAiKnowledgeRagAdapter(store);
        assertTrue(adapter.search(new KnowledgeRagPort.SearchRequest("", "user", "query", 5)).isEmpty());
        assertTrue(adapter.search(new KnowledgeRagPort.SearchRequest("tenant", "", "query", 5)).isEmpty());
        verifyNoInteractions(store);
    }

    @Test void nullIdentityDoesNotQueryVectorStore() {
        VectorStore store = mock(VectorStore.class);
        var adapter = new SpringAiKnowledgeRagAdapter(store);
        assertTrue(adapter.search(new KnowledgeRagPort.SearchRequest(null, null, "query", 5)).isEmpty());
        verifyNoInteractions(store);
    }

    @Test void tenantScopeIncludesTenantSharedAndOwnerDocuments() {
        VectorStore store = mock(VectorStore.class);
        var adapter = new SpringAiKnowledgeRagAdapter(store);
        String filter = adapter.permissionFilter(new KnowledgeRagPort.SearchRequest("tenant", "user", "query", 5, "tenant"));
        assertTrue(filter.contains("tenantKey == 'tenant'"));
        assertTrue(filter.contains("ownerKey == 'user'"));
        assertTrue(filter.contains("permissionScope == 'tenant'"));
        assertTrue(filter.contains("permissionScope == 'shared'"));
        assertTrue(filter.contains("status == 'published'"));
    }

    @Test void publicScopeDoesNotExposeOtherTenantDocuments() {
        VectorStore store = mock(VectorStore.class);
        var adapter = new SpringAiKnowledgeRagAdapter(store);
        String filter = adapter.permissionFilter(new KnowledgeRagPort.SearchRequest("tenant", "user", "query", 5, "public"));
        assertTrue(filter.contains("tenantKey == 'tenant'"));
        assertTrue(filter.contains("permissionScope == 'public'"));
        assertTrue(!filter.contains("ownerKey == 'user'"));
    }

    @Test void rejectsUnknownPermissionScope() {
        VectorStore store = mock(VectorStore.class);
        var adapter = new SpringAiKnowledgeRagAdapter(store);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> adapter.search(new KnowledgeRagPort.SearchRequest("tenant", "user", "query", 5, "admin")));
    }

    @Test void usesStableUuidStorageIdAndPreservesBusinessDocumentId() {
        VectorStore store = mock(VectorStore.class);
        var adapter = new SpringAiKnowledgeRagAdapter(store);
        adapter.index(new KnowledgeRagPort.IndexDocument("doc#0", "tenant", "owner", "content",
                Map.of("documentId", "doc", "chunkIndex", "0")));

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(store).add(captor.capture());
        Document indexed = (Document) ((List<?>) captor.getValue()).getFirst();
        assertDoesNotThrow(() -> java.util.UUID.fromString(indexed.getId()));
        assertEquals("doc", indexed.getMetadata().get("documentId"));
        assertEquals("doc#0", indexed.getMetadata().get("chunkId"));
    }
}
