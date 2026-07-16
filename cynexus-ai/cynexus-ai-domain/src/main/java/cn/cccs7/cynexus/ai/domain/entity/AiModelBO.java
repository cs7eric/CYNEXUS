package cn.cccs7.cynexus.ai.domain.entity;


import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelRuntimeOptions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelBO {

    private String modelCode;
    private String providerCode;
    private String providerIdentity;
    private String modelName;
    private String baseUrl;
    private String apiKey;
    private String apiKeyEnv;
    private String extra;
    private java.math.BigDecimal inputPricePerMillion;
    private java.math.BigDecimal outputPricePerMillion;
    private String pricingCurrency;
    private ModelCapability capability;
    private ModelRuntimeOptions runtimeOptions;
    private boolean enabled;
}
