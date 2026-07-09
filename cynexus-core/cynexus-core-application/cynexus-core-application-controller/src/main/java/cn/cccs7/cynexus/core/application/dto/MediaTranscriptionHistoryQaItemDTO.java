package cn.cccs7.cynexus.core.application.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionHistoryQaItemDTO {

    private String id;
    private String question;
    private String answer;
    private OffsetDateTime createdAt;
}
