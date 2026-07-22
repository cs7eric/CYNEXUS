package cn.cccs7.cynexus.ai.infra.basic.mapper;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentCapabilityPO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentCapabilityMapper extends BaseMapperX<AgentCapabilityPO> {
    int updateGovernance(@Param("capabilityCode") String capabilityCode,
                         @Param("enabled") boolean enabled,
                         @Param("availability") String availability,
                         @Param("priority") int priority);

    int markUnseenUnavailable(@Param("seenAt") java.time.OffsetDateTime seenAt);

    int markOlderVersionsUnavailable(@Param("capabilityCode") String capabilityCode,
                                     @Param("version") String version);
}
