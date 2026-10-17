package cn.cccs7.cynexus.ai.infra.ai.rag;

import cn.cccs7.cynexus.ai.domain.port.KnowledgeDocumentChunker;
import cn.cccs7.cynexus.ai.domain.port.KnowledgeIndexingPort;
import cn.cccs7.cynexus.ai.domain.port.KnowledgeRagPort;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

/** Splits documents before handing chunks to the permission-aware vector adapter. */
@Component
@ConditionalOnProperty(prefix = "cynexus.ai.rag", name = "enabled", havingValue = "true")
public class KnowledgeIndexingService implements KnowledgeIndexingPort {
    private final KnowledgeDocumentChunker chunker;
    private final ObjectProvider<KnowledgeRagPort> ragProvider;
    @Autowired
    public KnowledgeIndexingService(KnowledgeDocumentChunker chunker, ObjectProvider<KnowledgeRagPort> ragProvider) {
        this.chunker = chunker; this.ragProvider = ragProvider;
    }
    public KnowledgeIndexingService(KnowledgeDocumentChunker chunker, KnowledgeRagPort rag) {
        this(chunker, new SingletonProvider(rag));
    }
    @Override public void index(IndexCommand command) {
        KnowledgeRagPort rag = requireRag();
        if (command == null || command.content() == null || command.content().isBlank()) return;
        if (command.tenantKey() == null || command.tenantKey().isBlank()
                || command.ownerKey() == null || command.ownerKey().isBlank()) {
            throw new IllegalArgumentException("tenantKey and ownerKey are required for knowledge indexing");
        }
        rag.delete(command.documentId());
        String status = command.metadata() == null ? "published" : command.metadata().getOrDefault("status", "published");
        if (!"published".equalsIgnoreCase(status)) return;
        var chunks = chunker.chunk(command.content(), 1200, 120);
        for (int i = 0; i < chunks.size(); i++) {
            var metadata = new LinkedHashMap<String, String>();
            if (command.metadata() != null) metadata.putAll(command.metadata());
            metadata.put("tenantKey", command.tenantKey());
            metadata.put("ownerKey", command.ownerKey());
            metadata.put("chunkIndex", String.valueOf(i));
            metadata.put("documentId", command.documentId());
            metadata.putIfAbsent("status", "published");
            metadata.putIfAbsent("contentVersion", command.metadata() == null
                    ? "1" : command.metadata().getOrDefault("contentVersion", "1"));
            metadata.putIfAbsent("indexedAt", java.time.OffsetDateTime.now().toString());
            rag.index(new KnowledgeRagPort.IndexDocument(command.documentId() + "#" + i,
                    command.tenantKey(), command.ownerKey(), chunks.get(i), metadata));
        }
    }
    @Override public void delete(String documentId) { if (documentId != null && !documentId.isBlank()) requireRag().delete(documentId); }
    private KnowledgeRagPort requireRag() {
        KnowledgeRagPort rag = ragProvider.getIfAvailable();
        if (rag == null) throw new IllegalStateException("Knowledge RAG vector store is not configured");
        return rag;
    }
    private static final class SingletonProvider implements ObjectProvider<KnowledgeRagPort> {
        private final KnowledgeRagPort value;
        private SingletonProvider(KnowledgeRagPort value) { this.value = value; }
        public KnowledgeRagPort getObject() { return value; }
        public KnowledgeRagPort getObject(Object... args) { return value; }
        public KnowledgeRagPort getIfAvailable() { return value; }
        public KnowledgeRagPort getIfUnique() { return value; }
        public java.util.Iterator<KnowledgeRagPort> iterator() { return java.util.Collections.singleton(value).iterator(); }
        public java.util.stream.Stream<KnowledgeRagPort> stream() { return java.util.stream.Stream.of(value); }
        public java.util.stream.Stream<KnowledgeRagPort> orderedStream() { return stream(); }
    }
}
