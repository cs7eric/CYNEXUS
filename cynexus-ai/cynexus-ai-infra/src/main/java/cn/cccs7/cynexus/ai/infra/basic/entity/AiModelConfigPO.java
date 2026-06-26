package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import lombok.Data;

@Data
public class AiModelConfigPO extends BasePO {

    private String modelCode;
    private String providerCode;
    private String modelName;
    private Boolean enabled;
}
