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
@TableName(value = "tdx_sector_quote_detail_snapshot", autoResultMap = true)
public class TdxSectorQuoteDetailSnapshotPO extends BasePO {

    private LocalDate snapshotDate;
    private String code6;
    private String name;
    private String seedCode;
    private String source;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String payload;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String sectionCounts;
    private OffsetDateTime generatedAt;
}
