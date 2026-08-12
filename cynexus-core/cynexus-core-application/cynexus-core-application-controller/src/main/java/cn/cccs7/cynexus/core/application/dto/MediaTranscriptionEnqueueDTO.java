package cn.cccs7.cynexus.core.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionEnqueueDTO {

    private Boolean accepted;
    private String message;
    private String processId;
    private String status;
}
