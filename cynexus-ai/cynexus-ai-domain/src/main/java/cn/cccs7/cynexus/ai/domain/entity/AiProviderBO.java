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
public class AiProviderBO {

    private Long id;
    private String code;
    private String name;
    private String providerType;
    private String baseUrl;
    private String defaultModel;
    private String apiKey;
    private String apiKeyEnv;
    private String groupId;
    private String groupIdEnv;
    private Boolean enabled;
    private Integer timeoutSeconds;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
    private String models;
}
