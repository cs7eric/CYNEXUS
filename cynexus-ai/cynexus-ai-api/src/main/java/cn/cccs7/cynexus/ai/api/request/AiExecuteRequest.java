package cn.cccs7.cynexus.ai.api.request;


import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExecuteRequest {

    private String scenarioCode;
    private String userInput;
    private Map<String, Object> variables;
    private String preferredModelCode;
}
