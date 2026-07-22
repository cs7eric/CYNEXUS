package cn.cccs7.cynexus.ai.api.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentRunStepDTO {
    private Integer stepNo;
    private Integer attemptCount;
    private String kind;
    private String toolCode;
    private String status;
    private String summary;
    private String source;
    private String sourceUrl;
    private String citationJson;
    private String dataDate;
    private String inputJson;
    private String outputJson;
    private String errorMessage;
    private String failureCategory;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
}
