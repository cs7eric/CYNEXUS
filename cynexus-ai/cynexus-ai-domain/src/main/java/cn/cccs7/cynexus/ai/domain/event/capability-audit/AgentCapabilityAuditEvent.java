package cn.cccs7.cynexus.ai.domain.event.capabilityaudit;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

/** Auditable lifecycle change emitted when a runtime capability is registered or governed. */
@Value
@Builder
public class AgentCapabilityAuditEvent {
    Long id;
    String capabilityCode;
    String version;
    String action;
    Boolean enabled;
    String availability;
    String governanceAvailability;
    Integer priority;
    String reason;
    OffsetDateTime occurredAt;
}
