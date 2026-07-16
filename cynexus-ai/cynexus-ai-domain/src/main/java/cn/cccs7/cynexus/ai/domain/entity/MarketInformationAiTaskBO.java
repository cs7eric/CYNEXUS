package cn.cccs7.cynexus.ai.domain.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketInformationAiTaskBO {
    private Long id;
    private String taskType;
    private Long clusterId;
    private String clusterContentHash;
    private Integer attemptCount;
    private Integer maxAttempts;
    private String title;
    private String category;
    private List<Source> sources;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Source {
        private String sourceId;
        private String source;
        private String title;
        private String content;
        private String contentQuality;
        private List<String> deterministicImpact;
    }
}
