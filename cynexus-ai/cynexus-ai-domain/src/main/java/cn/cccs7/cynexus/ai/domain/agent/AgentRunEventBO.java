package cn.cccs7.cynexus.ai.domain.agent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentRunEventBO {
    String type;
    String runNo;
    String status;
    Integer stepNo;
    String toolCode;
    String summary;
    String answer;
    String delta;
    String errorMessage;
}
