package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "application_analysis_daily_snapshots", autoResultMap = true)
public class ApplicationAnalysisDailySnapshotPO extends BasePO {

    private Long targetId;
    private String targetKey;
    private LocalDate tradeDate;
    private String snapshotKind;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String payload;
    private String remark;
}
