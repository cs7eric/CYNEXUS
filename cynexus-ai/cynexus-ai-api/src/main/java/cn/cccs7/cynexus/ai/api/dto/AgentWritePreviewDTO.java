package cn.cccs7.cynexus.ai.api.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentWritePreviewDTO {
    String commandNo;
    String toolCode;
    String argumentDigest;
    String argumentsJson;
    String confirmationToken;
    OffsetDateTime expiresAt;
    String status;
}
