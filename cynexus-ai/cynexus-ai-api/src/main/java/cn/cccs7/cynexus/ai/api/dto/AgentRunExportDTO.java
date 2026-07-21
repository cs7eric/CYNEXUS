package cn.cccs7.cynexus.ai.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentRunExportDTO {
    private String format;
    private String filename;
    private String contentType;
    private String content;
}
