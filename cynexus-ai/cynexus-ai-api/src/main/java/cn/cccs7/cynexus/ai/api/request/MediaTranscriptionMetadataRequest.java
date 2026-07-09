package cn.cccs7.cynexus.ai.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionMetadataRequest {

    private String polishedText;
    private String summaryText;
    private String preferredModelCode;
}
