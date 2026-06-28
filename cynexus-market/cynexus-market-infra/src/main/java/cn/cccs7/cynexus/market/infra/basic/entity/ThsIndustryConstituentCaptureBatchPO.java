package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ths_industry_constituent_capture_batch", autoResultMap = true)
public class ThsIndustryConstituentCaptureBatchPO extends BasePO {

    private LocalDate snapshotDate;
    private String sourceType;
    private String status;
    private String source;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String requestedIndustryCodes;
    private Integer industryCount;
    private Integer rowCount;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String pageRowCounts;
    private OffsetDateTime fetchedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
}
