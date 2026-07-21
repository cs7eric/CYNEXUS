package cn.cccs7.cynexus.ai.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentStreamEventDTO {
    private String type;
    private String runNo;
    private String status;
    private Integer stepNo;
    private String toolCode;
    private String summary;
    private String answer;
    private String errorMessage;
}
