package cn.cccs7.cynexus.core.application.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionProcessRequest {

    private String url;
    private String title;
    private String platform;
    private String noteType;
}
