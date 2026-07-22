package cn.cccs7.cynexus.ai.infra.repository.task;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentTaskNotificationPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentTaskNotificationMapper;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentTaskNotificationEventKey;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Repository;

@Repository
public class AgentTaskNotificationRepository {
    private final AgentTaskNotificationMapper mapper;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public AgentTaskNotificationRepository(AgentTaskNotificationMapper mapper, SnowflakeIdGenerator idGenerator,
                                           ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    public boolean append(String eventKey, String eventType, cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO task,
                       Map<String, Object> payload) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(payload, "payload");
        String normalizedEventKey = AgentTaskNotificationEventKey.requireEventKey(eventKey);
        String normalizedEventType = AgentTaskNotificationEventKey.requireEventType(eventType);
        requireTaskField(task.getTaskNo(), "taskNo", 64);
        requireTaskField(task.getOwnerKey(), "ownerKey", 128);
        requireTaskField(task.getTenantKey(), "tenantKey", 128);
        requireTaskField(task.getPrincipalKey(), "principalKey", 128);
        AgentTaskNotificationPO notification = new AgentTaskNotificationPO();
        notification.setId(idGenerator.nextId());
        notification.setEventKey(normalizedEventKey);
        notification.setOwnerKey(task.getOwnerKey());
        notification.setTenantKey(task.getTenantKey());
        notification.setPrincipalKey(task.getPrincipalKey());
        notification.setTaskNo(task.getTaskNo());
        notification.setEventType(normalizedEventType);
        try {
            notification.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Task notification payload cannot be serialized", exception);
        }
        return mapper.insertIfAbsent(notification) > 0;
    }

    private void requireTaskField(String value, String field, int maxLength) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain 1-" + maxLength + " characters");
        }
    }
}
