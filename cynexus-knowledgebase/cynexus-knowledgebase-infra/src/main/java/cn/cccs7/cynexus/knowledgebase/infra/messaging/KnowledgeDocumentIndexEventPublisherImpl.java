package cn.cccs7.cynexus.knowledgebase.infra.messaging;

import cn.cccs7.cynexus.common.messaging.api.MessageDestination;
import cn.cccs7.cynexus.common.messaging.api.MessageEnvelope;
import cn.cccs7.cynexus.common.messaging.api.MessageType;
import cn.cccs7.cynexus.common.messaging.api.MessageTransport;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.index.KnowledgeDocumentIndexEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/** Publishes bounded document lifecycle payloads; vector indexing remains asynchronous. */
@Component
@ConditionalOnBean(MessageTransport.class)
public class KnowledgeDocumentIndexEventPublisherImpl implements KnowledgeDocumentIndexEventPublisher {
    public static final String EXCHANGE = "cynexus.event";
    public static final String UPSERT_KEY = "knowledge.document.changed.v1";
    public static final String DELETE_KEY = "knowledge.document.deleted.v1";
    private final MessageTransport transport;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public KnowledgeDocumentIndexEventPublisherImpl(MessageTransport transport) { this.transport = transport; }

    @Override public void documentChanged(KnowledgeDocumentBO document) {
        if (document == null || document.getId() == null) return;
        // An empty update must remove previously indexed chunks; otherwise stale
        // content remains searchable after the source document is cleared.
        if (document.getContent() == null || document.getContent().isBlank()) {
            documentDeleted(document.getId());
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentId", String.valueOf(document.getId()));
        payload.put("title", bounded(document.getTitle(), 500));
        payload.put("content", bounded(document.getContent(), 200_000));
        payload.put("status", value(document.getStatus(), "draft"));
        payload.put("contentFormat", value(document.getContentFormat(), "markdown"));
        payload.put("updatedAt", document.getUpdatedAt() == null ? null : document.getUpdatedAt().toString());
        payload.put("contentVersion", document.getUpdatedAt() == null ? "1" : document.getUpdatedAt().toEpochSecond());
        payload.put("tagIds", document.getTagIds() == null ? java.util.List.of() : document.getTagIds());
        payload.put("categoryIds", document.getCategoryIds() == null ? java.util.List.of() : document.getCategoryIds());
        payload.put("tags", document.getTags() == null ? java.util.List.of() : document.getTags().stream()
                .filter(java.util.Objects::nonNull).map(tag -> namedReference(tag.getId(), tag.getTagName())).toList());
        payload.put("categories", document.getCategories() == null ? java.util.List.of() : document.getCategories().stream()
                .filter(java.util.Objects::nonNull).map(category -> namedReference(category.getId(), category.getCategoryName())).toList());
        payload.put("extra", bounded(document.getExtra(), 4000));
        // Ownership is carried in the document extra metadata by the caller; never
        // publish an event that would make the AI index guess an identity.
        try {
            var extra = document.getExtra() == null ? null : mapper.readTree(document.getExtra());
            payload.put("tenantKey", value(document.getTenantKey(), extra == null ? "" : extra.path("tenantKey").asText("")));
            payload.put("ownerKey", value(document.getOwnerKey(), extra == null ? "" : extra.path("ownerKey").asText("")));
            payload.put("sourceType", extra == null ? "knowledge_document" : extra.path("sourceType").asText("knowledge_document"));
            payload.put("permissionScope", extra == null ? "owner" : extra.path("permissionScope").asText("owner"));
        } catch (Exception ignored) {
            payload.put("tenantKey", "");
            payload.put("ownerKey", "");
        }
        publish(UPSERT_KEY, String.valueOf(document.getId()), payload);
    }

    @Override public void documentDeleted(Long documentId) {
        if (documentId == null) return;
        publish(DELETE_KEY, String.valueOf(documentId), Map.of("documentId", String.valueOf(documentId)));
    }

    private void publish(String key, String aggregateId, Map<String, Object> payload) {
        try {
            transport.publish(new MessageDestination(EXCHANGE, key), new MessageEnvelope(
                    UUID.randomUUID().toString(), MessageType.EVENT, key, 1, "cynexus-knowledgebase",
                    "knowledge_document", aggregateId, Instant.now(), null, null, null,
                    mapper.writeValueAsString(payload), Map.of()));
        } catch (Exception exception) {
            throw new IllegalStateException("Knowledge document index event publish failed", exception);
        }
    }
    private String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private String bounded(String value, int max) { if (value == null) return null; return value.length() <= max ? value : value.substring(0, max); }
    private Map<String, Object> namedReference(Long id, String name) {
        Map<String, Object> value = new LinkedHashMap<>();
        if (id != null) value.put("id", id);
        if (name != null && !name.isBlank()) value.put("name", name);
        return value;
    }
}
