package cn.cccs7.cynexus.core.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionProgressBO {

    private String phase;
    private Integer progress;
    private Long downloadedBytes;
    private Long totalBytes;
    private Long processedBytes;
    private Integer etaSeconds;
    private Long speedBytesPerSec;
}
