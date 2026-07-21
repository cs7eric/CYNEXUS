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
@TableName(value = "ai_agent_run_step", autoResultMap = true)
public class AgentRunStepPO extends BasePO {
    @TableField("run_no") private String runNo;
    @TableField("step_no") private Integer stepNo;
    @TableField("attempt_count") private Integer attemptCount;
    private String kind;
    @TableField("tool_code") private String toolCode;
    @TableField(value = "input_json", typeHandler = JsonbTypeHandler.class) private String inputJson;
    @TableField(value = "output_json", typeHandler = JsonbTypeHandler.class) private String outputJson;
    private String status;
    private String summary;
    private String source;
    @TableField("data_date") private String dataDate;
    @TableField("error_message") private String errorMessage;
    @TableField("started_at") private OffsetDateTime startedAt;
    @TableField("finished_at") private OffsetDateTime finishedAt;
}
