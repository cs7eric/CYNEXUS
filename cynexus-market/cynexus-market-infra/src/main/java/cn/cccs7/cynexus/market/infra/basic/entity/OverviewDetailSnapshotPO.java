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
@TableName(value = "mkt_overview_detail_snapshot", autoResultMap = true)
public class OverviewDetailSnapshotPO extends BasePO {

    private LocalDate tradeDate;
    private String benchmarkCode;
    private String sourceTable;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String payload;
    private String source;
    private OffsetDateTime generatedAt;
}
