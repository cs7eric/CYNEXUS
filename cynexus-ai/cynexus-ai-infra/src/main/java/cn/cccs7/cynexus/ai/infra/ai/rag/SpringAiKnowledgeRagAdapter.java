package cn.cccs7.cynexus.ai.infra.ai.rag;

import cn.cccs7.cynexus.ai.domain.port.KnowledgeRagPort;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/** RAG adapter; metadata filters enforce tenant/owner isolation before retrieval. */
public class SpringAiKnowledgeRagAdapter implements KnowledgeRagPort {
    private final VectorStore vectorStore;
    public SpringAiKnowledgeRagAdapter(VectorStore vectorStore) { this.vectorStore = vectorStore; }
    @Override public void index(IndexDocument document) {
        if (document == null || document.content() == null || document.content().isBlank()) return;
        if (document.tenantKey() == null || document.tenantKey().isBlank()
                || document.ownerKey() == null || document.ownerKey().isBlank()) {
            throw new IllegalArgumentException("tenantKey and ownerKey are required for vector indexing");
        }
        var metadata = new java.util.LinkedHashMap<String, Object>();
        if (document.metadata() != null) metadata.putAll(document.metadata());
        metadata.put("tenantKey", document.tenantKey());
        metadata.put("ownerKey", document.ownerKey());
        metadata.putIfAbsent("documentId", document.documentId());
        // PgVectorStore's default schema uses UUID primary keys. Keep the
        // business/chunk id in metadata and derive a stable storage id so
        // re-indexing the same chunk updates the same vector row.
        String storageId = UUID.nameUUIDFromBytes(
                ("cynexus:knowledge:" + document.documentId()).getBytes(StandardCharsets.UTF_8)).toString();
        metadata.putIfAbsent("chunkId", document.documentId());
        vectorStore.add(List.of(new Document(storageId, document.content(), metadata)));
    }
    @Override public void delete(String documentId) {
        if (documentId == null || documentId.isBlank()) return;
        vectorStore.delete(new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder()
                .eq("documentId", documentId).build());
    }
    @Override public List<RetrievedChunk> search(SearchRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()
                || request.tenantKey() == null || request.tenantKey().isBlank()
                || request.principalKey() == null || request.principalKey().isBlank()) return List.of();
        String filter = permissionFilter(request);
        org.springframework.ai.vectorstore.SearchRequest vectorRequest = org.springframework.ai.vectorstore.SearchRequest.builder().query(request.query()).topK(Math.max(1, Math.min(50, request.topK()))).filterExpression(filter).build();
        return vectorStore.similaritySearch(vectorRequest)
                .stream().map(d -> {
                    var metadata = stringify(d.getMetadata());
                    String documentId = metadata.getOrDefault("documentId", metadata.getOrDefault("chunkId", d.getId()));
                    return new RetrievedChunk(documentId, d.getText(), d.getScore() == null ? 0d : d.getScore(), metadata);
                }).toList();
    }

    String permissionFilter(SearchRequest request) {
        String scope = request.permissionScope() == null ? "owner" : request.permissionScope().trim().toLowerCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("owner", "tenant", "shared", "public").contains(scope)) {
            throw new IllegalArgumentException("unsupported permissionScope");
        }
        String tenant = "tenantKey == '" + escape(request.tenantKey()) + "'";
        String owner = "ownerKey == '" + escape(request.principalKey()) + "'";
        String visibility = switch (scope) {
            case "public" -> "permissionScope == 'public'";
            case "tenant", "shared" -> "(" + owner + " || permissionScope == 'tenant' || permissionScope == 'shared')";
            default -> owner;
        };
        return "(" + tenant + " && " + visibility + ") && status == 'published'";
    }
    private java.util.Map<String, String> stringify(java.util.Map<String, Object> source) {
        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, String.valueOf(value)));
        return result;
    }
    private String escape(String value) { return value == null ? "" : value.replace("'", "''"); }
}
