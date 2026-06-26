package cn.cccs7.cynexus.ai.api.dto;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExecuteDTO {

    private String executionNo;
    private String scenarioCode;
    private String modelCode;
    private String status;
    private String content;
    private List<String> toolCalls;
}
