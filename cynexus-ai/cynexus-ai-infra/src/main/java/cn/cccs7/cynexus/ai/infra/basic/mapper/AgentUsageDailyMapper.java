package cn.cccs7.cynexus.ai.infra.basic.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentUsageDailyMapper {
    @Insert("INSERT INTO cynexus_appl_ai.ai_agent_usage_daily (owner_key, usage_date, tenant_key, principal_key, request_count, reserved_tokens, updated_at) "
            + "VALUES (#{ownerKey}, #{usageDate}, #{tenantKey}, #{principalKey}, 1, #{estimatedTokens}, now()) "
            + "ON CONFLICT (owner_key, usage_date) DO UPDATE SET request_count=ai_agent_usage_daily.request_count+1, "
            + "reserved_tokens=ai_agent_usage_daily.reserved_tokens+#{estimatedTokens}, updated_at=now() "
            + "WHERE ai_agent_usage_daily.request_count < #{maxRequests} "
            + "AND ai_agent_usage_daily.total_tokens + ai_agent_usage_daily.reserved_tokens + #{estimatedTokens} <= #{maxTokens}")
    int reserve(@Param("ownerKey") String ownerKey, @Param("usageDate") LocalDate usageDate,
                @Param("tenantKey") String tenantKey, @Param("principalKey") String principalKey,
                @Param("estimatedTokens") int estimatedTokens, @Param("maxRequests") int maxRequests, @Param("maxTokens") int maxTokens);

    @Update("UPDATE cynexus_appl_ai.ai_agent_usage_daily SET reserved_tokens=GREATEST(0, reserved_tokens-#{reservedTokens}), "
            + "input_tokens=input_tokens+#{inputTokens}, output_tokens=output_tokens+#{outputTokens}, total_tokens=total_tokens+#{totalTokens}, "
            + "estimated_cost=estimated_cost+#{estimatedCost}, estimated_usage_count=estimated_usage_count+#{estimatedUsageCount}, updated_at=now() "
            + "WHERE owner_key=#{ownerKey} AND usage_date=#{usageDate}")
    int record(@Param("ownerKey") String ownerKey, @Param("usageDate") LocalDate usageDate,
               @Param("reservedTokens") int reservedTokens, @Param("inputTokens") int inputTokens,
               @Param("outputTokens") int outputTokens, @Param("totalTokens") int totalTokens,
               @Param("estimatedCost") BigDecimal estimatedCost, @Param("estimatedUsageCount") int estimatedUsageCount);
}
