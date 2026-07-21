package cn.cccs7.cynexus.ai.domain.agent;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;

@Value
@Builder(toBuilder = true)
public class AgentRunEventBO {
    Integer sequenceNo;
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
