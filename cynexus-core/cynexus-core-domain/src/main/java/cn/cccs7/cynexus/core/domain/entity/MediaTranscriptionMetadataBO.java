package cn.cccs7.cynexus.core.domain.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionMetadataBO {

    private String title;
    private List<String> categories;
    private List<String> tags;
    private String sourceUrl;
    private String downloadUrl;
    private String platform;
    private Integer duration;
    private String noteType;
    private String audioUrl;
    private String originalUrl;
}
