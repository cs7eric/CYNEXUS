package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_industry_application_target_catalog")
public class IndustryApplicationTargetCatalogPO extends BasePO {

    private String targetType;
    private String symbol;
    private String code6;
    private String exchange;
    private String name;
    private String source;
    private String remark;
}
