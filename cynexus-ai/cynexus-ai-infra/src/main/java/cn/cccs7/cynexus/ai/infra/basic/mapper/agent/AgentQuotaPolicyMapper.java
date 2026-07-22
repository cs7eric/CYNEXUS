package cn.cccs7.cynexus.ai.infra.basic.mapper.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentQuotaPolicyPO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgentQuotaPolicyMapper extends BaseMapperX<AgentQuotaPolicyPO> {
    @Select("SELECT * FROM cynexus_appl_ai.ai_agent_quota_policy "
            + "WHERE deleted_at IS NULL AND enabled = true "
            + "AND (tenant_key = #{tenantKey} OR tenant_key IS NULL) "
            + "AND (principal_key = #{principalKey} OR principal_key IS NULL) "
            + "ORDER BY CASE WHEN tenant_key = #{tenantKey} AND principal_key = #{principalKey} THEN 1 "
            + "WHEN tenant_key = #{tenantKey} AND principal_key IS NULL THEN 2 "
            + "WHEN tenant_key IS NULL AND principal_key IS NULL THEN 3 ELSE 4 END, effective_at DESC LIMIT 1")
    AgentQuotaPolicyPO findApplicable(@Param("tenantKey") String tenantKey, @Param("principalKey") String principalKey);
}
