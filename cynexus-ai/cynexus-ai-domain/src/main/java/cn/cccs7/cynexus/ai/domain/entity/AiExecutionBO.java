package cn.cccs7.cynexus.ai.domain.entity;


import cn.cccs7.cynexus.ai.domain.execution.AiExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExecutionBO {

    private String executionNo;
    private AiScenarioBO scenario;
    private String modelCode;
    private AiExecutionStatus status;
    private String content;
    private List<String> toolCalls;
}
