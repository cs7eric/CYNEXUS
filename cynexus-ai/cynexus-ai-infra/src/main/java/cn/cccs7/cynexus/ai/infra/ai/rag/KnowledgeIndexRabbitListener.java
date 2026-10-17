package cn.cccs7.cynexus.ai.infra.ai.rag;

import cn.cccs7.cynexus.ai.domain.port.KnowledgeIndexingPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Consumes knowledge lifecycle events and delegates indexing to the governed domain port. */
@Component
@ConditionalOnBean(KnowledgeIndexingPort.class)
public class KnowledgeIndexRabbitListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeIndexRabbitListener.class);
    private final KnowledgeIndexingPort indexing;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public KnowledgeIndexRabbitListener(KnowledgeIndexingPort indexing) { this.indexing = indexing; }

    @RabbitListener(queues = KnowledgeIndexRabbitTopologyConfiguration.UPSERT_QUEUE,
            ackMode = "MANUAL", containerFactory = "knowledgeIndexRabbitListenerContainerFactory")
    public void upsert(Message message, Channel channel) throws Exception {
        long tag = message.getMessageProperties().getDeliveryTag();
        String documentId = "";
        try {
            JsonNode payload = readBusinessPayload(message.getBody());
            documentId = text(payload, "documentId");
            String tenantKey = text(payload, "tenantKey");
            String ownerKey = text(payload, "ownerKey");
            if (documentId.isBlank() || tenantKey.isBlank() || ownerKey.isBlank()) {
                throw new IllegalArgumentException("knowledge index event document identity is required");
            }
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("title", text(payload, "title"));
            metadata.put("status", text(payload, "status"));
            metadata.put("contentFormat", text(payload, "contentFormat"));
            metadata.put("updatedAt", text(payload, "updatedAt"));
            metadata.put("contentVersion", text(payload, "contentVersion"));
            metadata.put("sourceType", text(payload, "sourceType"));
            metadata.put("permissionScope", text(payload, "permissionScope"));
            metadata.put("tagIds", payload.path("tagIds").toString());
            metadata.put("categoryIds", payload.path("categoryIds").toString());
            metadata.put("tags", payload.path("tags").toString());
            metadata.put("categories", payload.path("categories").toString());
            indexing.index(new KnowledgeIndexingPort.IndexCommand(documentId, tenantKey, ownerKey,
                    text(payload, "content"), metadata));
            channel.basicAck(tag, false);
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Rejecting malformed knowledge index event, deliveryTag={}", tag, exception);
            channel.basicNack(tag, false, false);
        } catch (IllegalArgumentException exception) {
            // Only the contract validation above is permanent. Downstream vector
            // clients can also use IllegalArgumentException for provider or SQL
            // failures, which must go through the retry policy instead.
            if ("knowledge index event document identity is required".equals(exception.getMessage())) {
                LOGGER.warn("Rejecting knowledge index event without document identity, deliveryTag={}", tag);
                channel.basicNack(tag, false, false);
                return;
            }
            LOGGER.warn("Knowledge index failed for documentId={}; retrying through listener policy", documentId, exception);
            throw exception;
        } catch (Exception exception) {
            // Transient vector/provider failures are retried by the container advice chain.
            LOGGER.warn("Knowledge index failed for documentId={}; retrying through listener policy", documentId, exception);
            throw exception;
        }
    }

    @RabbitListener(queues = KnowledgeIndexRabbitTopologyConfiguration.DELETE_QUEUE,
            ackMode = "MANUAL", containerFactory = "knowledgeIndexRabbitListenerContainerFactory")
    public void delete(Message message, Channel channel) throws Exception {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            JsonNode payload = readBusinessPayload(message.getBody());
            String documentId = text(payload, "documentId");
            if (!documentId.isBlank()) {
                indexing.delete(documentId);
            }
            channel.basicAck(tag, false);
        } catch (IllegalArgumentException | JsonProcessingException exception) { channel.basicNack(tag, false, false); }
        catch (Exception exception) { throw exception; }
    }

    /** Accept the transport's raw payload and complete-envelope integrations. */
    private JsonNode readBusinessPayload(byte[] body) throws JsonProcessingException {
        JsonNode root = mapper.readTree(new String(body, StandardCharsets.UTF_8));
        if (root == null || !root.isObject()) {
            throw new JsonProcessingException("knowledge index event must be a JSON object") { };
        }
        if (root.has("documentId")) {
            return root;
        }
        JsonNode nested = root.get("payloadJson");
        if (nested == null || nested.isNull()) {
            nested = root.get("payload");
        }
        if (nested == null || nested.isNull()) {
            return root;
        }
        if (nested.isTextual()) {
            JsonNode parsed = mapper.readTree(nested.asText());
            if (parsed == null || !parsed.isObject()) {
                throw new JsonProcessingException("knowledge index event payload must be a JSON object") { };
            }
            return parsed;
        }
        if (nested.isObject()) {
            return nested;
        }
        throw new JsonProcessingException("knowledge index event payload must be a JSON object") { };
    }

    private String text(JsonNode node, String name) { return node == null ? "" : node.path(name).asText("").trim(); }
}
