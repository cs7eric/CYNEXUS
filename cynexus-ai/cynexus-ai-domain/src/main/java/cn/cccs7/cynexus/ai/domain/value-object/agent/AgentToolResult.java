package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import cn.cccs7.cynexus.ai.domain.valueobject.citation.AgentCitation;
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
    String sourceUrl;
    List<String> warnings;
    List<AgentCitation> citations;

    public static AgentToolResult success(String toolCode, String outputJson, String summary,
                                          String dataDate, String source, List<String> warnings) {
        return AgentToolResult.builder()
                .toolCode(toolCode)
                .status("SUCCEEDED")
                .outputJson(outputJson)
                .summary(summary)
                .dataDate(dataDate)
                .source(source)
                .sourceUrl(null)
                .warnings(warnings == null ? List.of() : List.copyOf(warnings))
                .citations(List.of())
                .build();
    }

    public static AgentToolResult failure(String toolCode, String summary, String source, String warning) {
        return AgentToolResult.builder()
                .toolCode(toolCode)
                .status("FAILED")
                .outputJson("{}")
                .summary(summary)
                .source(source)
                .sourceUrl(null)
                .warnings(warning == null || warning.isBlank() ? List.of() : List.of(warning))
                .citations(List.of())
                .build();
    }
}
