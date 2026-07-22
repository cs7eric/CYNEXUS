package cn.cccs7.cynexus.ai.infra.basic.mapper.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentTaskNotificationPO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentTaskNotificationMapper extends BaseMapperX<AgentTaskNotificationPO> {
    @Insert("INSERT INTO cynexus_appl_ai.ai_agent_task_notification "
            + "(id, event_key, owner_key, tenant_key, principal_key, task_no, event_type, payload_json, status, attempt_count, next_attempt_at, created_at, updated_at) "
            + "VALUES (#{id}, #{eventKey}, #{ownerKey}, #{tenantKey}, #{principalKey}, #{taskNo}, #{eventType}, #{payloadJson,typeHandler=cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler}, 'PENDING', 0, now(), COALESCE(#{createdAt}, now()), now()) "
            + "ON CONFLICT (event_key) DO NOTHING")
    int insertIfAbsent(AgentTaskNotificationPO notification);
}
