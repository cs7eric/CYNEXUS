package cn.cccs7.cynexus.core.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionTaskDetailDTO {

    private String taskId;
    private String status;
    private String transcript;
    private String polished;
    private String summary;
    private MediaTranscriptionMetadataDTO metadata;
    private String fileName;
    private String error;
    private MediaTranscriptionProgressDTO downloadProgress;
    private MediaTranscriptionProgressDTO intakeProgress;
}
