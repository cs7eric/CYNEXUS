package cn.cccs7.cynexus.ai.infra.ai.rag;

import static org.junit.jupiter.api.Assertions.*;
import cn.cccs7.cynexus.ai.domain.port.KnowledgeRagPort;
import cn.cccs7.cynexus.ai.domain.port.KnowledgeIndexingPort;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class KnowledgeIndexingServiceTest {
    @Test void attachesTenantOwnerAndChunkMetadata() {
        var documents = new ArrayList<KnowledgeRagPort.IndexDocument>();
        var deleted = new ArrayList<String>();
        KnowledgeRagPort rag = new KnowledgeRagPort() {
            public void index(IndexDocument document) { documents.add(document); }
            public void delete(String documentId) { deleted.add(documentId); }
            public java.util.List<RetrievedChunk> search(SearchRequest request) { return java.util.List.of(); }
        };
        new KnowledgeIndexingService(new KnowledgeDocumentChunkerImpl(), rag)
                .index(new KnowledgeIndexingPort.IndexCommand("doc", "tenant", "owner", "content", null));
        assertEquals(1, documents.size());
        assertEquals("tenant", documents.getFirst().metadata().get("tenantKey"));
        assertEquals("owner", documents.getFirst().metadata().get("ownerKey"));
        assertEquals("doc", documents.getFirst().metadata().get("documentId"));
        assertEquals(java.util.List.of("doc"), deleted);
    }

    @Test void rejectsIndexingWithoutIdentity() {
        KnowledgeRagPort rag = new KnowledgeRagPort() {
            public void index(IndexDocument document) { fail("must not index without identity"); }
            public void delete(String documentId) { fail("must not mutate without identity"); }
            public java.util.List<RetrievedChunk> search(SearchRequest request) { return java.util.List.of(); }
        };
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeIndexingService(new KnowledgeDocumentChunkerImpl(), rag)
                .index(new KnowledgeIndexingPort.IndexCommand("doc", "", "owner", "content", null)));
    }

    @Test void removesOldChunksAndSkipsDraftDocuments() {
        var indexed = new ArrayList<KnowledgeRagPort.IndexDocument>();
        var deleted = new ArrayList<String>();
        KnowledgeRagPort rag = new KnowledgeRagPort() {
            public void index(IndexDocument document) { indexed.add(document); }
            public void delete(String documentId) { deleted.add(documentId); }
            public java.util.List<RetrievedChunk> search(SearchRequest request) { return java.util.List.of(); }
        };
        new KnowledgeIndexingService(new KnowledgeDocumentChunkerImpl(), rag)
                .index(new KnowledgeIndexingPort.IndexCommand("doc", "tenant", "owner", "draft text",
                        java.util.Map.of("status", "draft", "categoryIds", "[1]", "tagIds", "[2]")));
        assertTrue(indexed.isEmpty());
        assertEquals(java.util.List.of("doc"), deleted);
    }

}
