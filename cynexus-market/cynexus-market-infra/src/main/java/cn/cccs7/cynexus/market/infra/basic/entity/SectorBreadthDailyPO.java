package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "msi_sector_breadth_daily", autoResultMap = true)
public class SectorBreadthDailyPO extends BasePO {

    private LocalDate tradeDate;
    private Integer advancing;
    private Integer declining;
    private Integer flat;
    private Integer total;
    private BigDecimal advancePct;
    private String source;
    private Integer elapsedMs;
    private OffsetDateTime ingestedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
