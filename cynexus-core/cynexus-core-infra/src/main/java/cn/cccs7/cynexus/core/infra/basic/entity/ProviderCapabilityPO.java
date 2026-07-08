package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "provider_capability_catalog", autoResultMap = true)
public class ProviderCapabilityPO extends BasePO {

    private String capabilityCode;
    private String name;
    private String category;
    private String providerCode;
    private String providerName;
    private String capabilityType;
    private String sourceLayer;
    private String targetStore;
    private String status;
    private String dataGranularity;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String storageTables;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String updateJobs;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String upstreamEndpoints;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String usedBy;
    private String rateLimitPolicy;
    private String freshnessPolicy;
    private String antiScrapingNotes;
    private String description;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String params;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private Integer sortOrder;
    private String remark;
}
