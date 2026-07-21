package cn.cccs7.cynexus.ai.domain.agent;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentToolResult {

    String toolCode;
    String status;
    String outputJson;
    String summary;
    String dataDate;
    String source;
    List<String> warnings;

    public static AgentToolResult success(String toolCode, String outputJson, String summary,
                                          String dataDate, String source, List<String> warnings) {
        return AgentToolResult.builder()
                .toolCode(toolCode)
                .status("SUCCEEDED")
                .outputJson(outputJson)
                .summary(summary)
                .dataDate(dataDate)
                .source(source)
                .warnings(warnings == null ? List.of() : List.copyOf(warnings))
                .build();
    }

    public static AgentToolResult failure(String toolCode, String summary, String source, String warning) {
        return AgentToolResult.builder()
                .toolCode(toolCode)
                .status("FAILED")
                .outputJson("{}")
                .summary(summary)
                .source(source)
                .warnings(warning == null || warning.isBlank() ? List.of() : List.of(warning))
                .build();
    }
}
