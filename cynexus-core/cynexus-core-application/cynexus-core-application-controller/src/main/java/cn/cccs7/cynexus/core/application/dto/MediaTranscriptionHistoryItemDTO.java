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
public class MediaTranscriptionHistoryItemDTO {

    private String id;
    private String title;
    private OffsetDateTime createdAt;
    private String taskId;
    private String status;
    private String fileName;
    private String dataFile;
}
