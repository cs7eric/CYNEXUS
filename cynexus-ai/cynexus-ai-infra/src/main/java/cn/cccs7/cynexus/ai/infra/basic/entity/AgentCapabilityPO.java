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
@TableName(value = "ai_agent_capability", autoResultMap = true)
public class AgentCapabilityPO extends BasePO {
    @TableField("capability_code") private String capabilityCode;
    private String version;
    private String name;
    private String module;
    private String description;
    @TableField(value = "input_schema_json", typeHandler = JsonbTypeHandler.class) private String inputSchemaJson;
    @TableField(value = "output_schema_json", typeHandler = JsonbTypeHandler.class) private String outputSchemaJson;
    @TableField("is_read_only") private Boolean readOnly;
    @TableField("requires_confirmation") private Boolean requiresConfirmation;
    @TableField("tenant_scope") private String tenantScope;
    private String source;
    @TableField("source_url") private String sourceUrl;
    private String freshness;
    private String availability;
    @TableField("governance_availability") private String governanceAvailability;
    @TableField("is_enabled") private Boolean enabled;
    private Integer priority;
    @TableField("prompt_key") private String promptKey;
    @TableField("runtime_entry") private String runtimeEntry;
    @TableField("route_group") private String routeGroup;
    @TableField("last_seen_at") private OffsetDateTime lastSeenAt;
    @TableField("stale_at") private OffsetDateTime staleAt;
}
