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
@TableName(value = "mkt_style_sector_snapshot", autoResultMap = true)
public class StyleSectorSnapshotPO extends BasePO {

    private LocalDate tradeDate;
    private String name;
    private Integer rankByChange;
    private BigDecimal changePct;
    private Integer validSize;
    private Integer sampleSize;
    private String source;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String payload;
    private OffsetDateTime ingestedAt;
}
