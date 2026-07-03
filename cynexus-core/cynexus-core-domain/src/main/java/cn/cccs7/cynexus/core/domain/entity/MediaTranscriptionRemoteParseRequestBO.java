package cn.cccs7.cynexus.core.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionRemoteParseRequestBO {

    private String downloadUrl;
    private String title;
    private String sourceUrl;
    private String platform;
    private Integer duration;
    private String noteType;
    private String audioUrl;
    private String originalUrl;
}
