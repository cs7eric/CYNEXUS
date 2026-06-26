package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;

public class AiModelConfigPO extends BasePO {

    private String modelCode;
    private String providerCode;
    private String modelName;
    private Boolean enabled;

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
