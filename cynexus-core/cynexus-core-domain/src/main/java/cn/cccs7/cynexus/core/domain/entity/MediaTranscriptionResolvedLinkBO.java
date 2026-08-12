package cn.cccs7.cynexus.core.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionResolvedLinkBO {

    private String title;
    private String platform;
    private Integer duration;
    private String noteType;
    private String originDownloadVideoUrl;
    private String originDownloadAudioUrl;
    private String downloadVideoUrl;
    private String downloadAudioUrl;
    private String originalUrl;
}
