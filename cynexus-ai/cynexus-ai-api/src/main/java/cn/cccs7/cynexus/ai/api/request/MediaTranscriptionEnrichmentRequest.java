package cn.cccs7.cynexus.ai.api.request;

import lombok.Data;

@Data
public class MediaTranscriptionEnrichmentRequest {
    private String transcript;
    private String title;
    private String platform;
    private Integer duration;
    private String preferredModelCode;
}
