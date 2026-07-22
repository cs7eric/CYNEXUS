package cn.cccs7.cynexus.ai.domain.entity;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentQuotaPolicyBO {
    private Long id;
    private String tenantKey;
    private String principalKey;
    private Integer dailyRequestLimit;
    private Long dailyTokenLimit;
    private Boolean enabled;
    private OffsetDateTime effectiveAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
