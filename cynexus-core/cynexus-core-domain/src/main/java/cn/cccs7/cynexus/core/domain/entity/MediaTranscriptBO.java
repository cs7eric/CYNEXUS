package cn.cccs7.cynexus.core.domain.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptBO {

    private String transcriptId;
    private String sourceUri;
    private String transcript;
    private String language;
    private BigDecimal durationSeconds;
    private String metadata;
    private OffsetDateTime createdAt;
}
