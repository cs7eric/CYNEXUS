package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sec_stock_basic")
public class StockBasicPO extends BasePO {

    private String code;
    private String name;
    private String market;
    private String board;
    @TableField("is_st")
    private Boolean st;
    private BigDecimal limitUpThreshold;
    @TableField("is_active")
    private Boolean active;
    private String source;
    private OffsetDateTime fetchedAt;
    private String industry;
    private BigDecimal totalMarketCap;
    private BigDecimal floatMarketCap;
    private String capStyle;
    private String sectorIndexSymbol;
    private String sectorIndexName;
    private String industryQuery;
}
