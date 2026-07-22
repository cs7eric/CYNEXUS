package cn.cccs7.cynexus.ai.api.dto;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class AgentStreamEventDTO {
    private Integer sequenceNo;
    private String traceId;
    private String type;
    private String runNo;
    private String status;
    private Integer stepNo;
    private String toolCode;
    private String summary;
    private String answer;
    private String delta;
    private String errorMessage;
    private OffsetDateTime occurredAt;
}
