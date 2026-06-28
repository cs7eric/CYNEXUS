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
@TableName(value = "mkt_heatmap_snapshot", autoResultMap = true)
public class HeatmapSnapshotPO extends BasePO {

    private LocalDate snapshotDate;
    private String kind;
    private String source;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String payload;
    private Integer itemCount;
    private OffsetDateTime generatedAt;
}
