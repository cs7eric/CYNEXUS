package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "application_analysis_result_history", autoResultMap = true)
public class ApplicationAnalysisResultHistoryPO extends BasePO {

    private Long targetId;
    private String targetKey;
    private String historyKey;
    private String sourceKind;
    private OffsetDateTime analysisRunAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String payload;
    private String remark;
}
