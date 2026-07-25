package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_providers", autoResultMap = true)
public class AiProviderPO extends BasePO {

    private String code;
    private String name;
    @TableField("provider_type")
    private String providerType;
    private String baseUrl;
    private String defaultModel;
    private String apiKey;
    private String apiKeyEnv;
    private String groupId;
    private String groupIdEnv;
    @TableField("is_enabled")
    private Boolean enabled;
    private Integer timeoutSeconds;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String models;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String modelConfigs;
    @TableField("input_price_per_million")
    private java.math.BigDecimal inputPricePerMillion;
    @TableField("output_price_per_million")
    private java.math.BigDecimal outputPricePerMillion;
    @TableField("pricing_currency")
    private String pricingCurrency;
}
