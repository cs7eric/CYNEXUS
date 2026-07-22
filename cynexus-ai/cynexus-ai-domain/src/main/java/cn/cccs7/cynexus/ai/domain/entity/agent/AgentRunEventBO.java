package cn.cccs7.cynexus.ai.domain.entity.agent;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;

@Value
@Builder(toBuilder = true)
public class AgentRunEventBO {
    Integer sequenceNo;
    String traceId;
    String type;
    String runNo;
    String status;
    Integer stepNo;
    String toolCode;
    String summary;
    String answer;
    String delta;
    String errorMessage;
    OffsetDateTime occurredAt;
}
