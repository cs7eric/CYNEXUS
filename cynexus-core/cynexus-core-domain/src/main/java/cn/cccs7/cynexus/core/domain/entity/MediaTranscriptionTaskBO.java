package cn.cccs7.cynexus.core.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionTaskBO {

    private String taskId;
    private String status;
    private String transcript;
    private String polished;
    private String summary;
    private MediaTranscriptionMetadataBO metadata;
    private String fileName;
    private String error;
    private MediaTranscriptionProgressBO downloadProgress;
    private MediaTranscriptionProgressBO intakeProgress;
}
