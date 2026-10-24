package cn.cccs7.cynexus.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiVisionTaskEnqueueDTO {
    private boolean accepted;
    private String message;
    private String processId;
    private String status;
    private String workflowType;
    private String workflowKey;
    private String pollUrl;
}
