package cn.cccs7.cynexus.ai.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExecutionCreatedEvent {

    private String executionNo;
    private String scenarioCode;
}
