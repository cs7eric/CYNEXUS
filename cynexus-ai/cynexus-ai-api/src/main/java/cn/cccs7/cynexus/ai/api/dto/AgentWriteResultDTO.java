package cn.cccs7.cynexus.ai.api.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentWriteResultDTO {
    String commandNo;
    String status;
    String resultJson;
    String errorMessage;
}
