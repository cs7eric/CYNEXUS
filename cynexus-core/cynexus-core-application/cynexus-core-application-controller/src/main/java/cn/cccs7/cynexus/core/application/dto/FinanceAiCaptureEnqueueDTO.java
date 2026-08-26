package cn.cccs7.cynexus.core.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceAiCaptureEnqueueDTO {

    private Boolean accepted;
    private String message;
    private String processId;
    private String status;
    private String workflowType;
    private String workflowKey;
    private String pollUrl;
}
