package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_industry_application_config")
public class IndustryApplicationConfigPO extends BasePO {

    private String configKey;
    private Integer version;
    private Integer horizonDays;
    private Integer horizonSegments;
    private String source;
    private String remark;
}
