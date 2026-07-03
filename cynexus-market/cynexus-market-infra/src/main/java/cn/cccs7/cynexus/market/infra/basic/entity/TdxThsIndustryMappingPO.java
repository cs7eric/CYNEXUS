package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tdx_ths_industry_mapping")
public class TdxThsIndustryMappingPO extends BasePO {

    private String tdxCode6;
    private String tdxName;
    private String thsCode;
    private String thsName;
    private String matchType;
    private BigDecimal confidence;
    private Integer priority;
    private Boolean isPrimary;
    private String remark;
    private String source;
}
