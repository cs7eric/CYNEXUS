package cn.cccs7.cynexus.ai.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionSummaryRequest {

    private String text;
    private String title;
    private String platform;
    private Integer duration;
    private String preferredModelCode;
}
