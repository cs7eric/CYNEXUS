package cn.cccs7.cynexus.ai.application.command;


import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteScenarioCommand {

    private String scenarioCode;
    private String userInput;
    private Map<String, Object> variables;
    private String preferredModelCode;
}
