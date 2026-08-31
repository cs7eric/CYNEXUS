package cn.cccs7.cynexus.ai.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MediaTranscriptionEnrichmentDTO {
    String polishedText;
    String summary;
    String shortSummary;
    List<String> keyPoints;
    List<String> topics;
    List<String> methodology;
    List<String> actionItems;
    List<String> risks;
    String title;
    List<String> categories;
    List<String> tags;
}
