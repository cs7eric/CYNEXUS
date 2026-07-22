package cn.cccs7.cynexus.ai.infra.basic.mapper;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentTaskPO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentTaskMapper extends BaseMapperX<AgentTaskPO> {
    int pause(@Param("ownerKey") String ownerKey, @Param("taskNo") String taskNo);

    int resume(@Param("ownerKey") String ownerKey, @Param("taskNo") String taskNo);

    int cancel(@Param("ownerKey") String ownerKey, @Param("taskNo") String taskNo);

    List<AgentTaskPO> claimDue(@Param("workerId") String workerId, @Param("now") OffsetDateTime now,
                               @Param("leaseUntil") OffsetDateTime leaseUntil, @Param("limit") int limit);

    int complete(AgentTaskPO task);

    int retry(AgentTaskPO task);

    int deadLetter(AgentTaskPO task);
}
