package cn.cccs7.cynexus.ai.infra.basic.mapper.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentTaskPO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentTaskMapper extends BaseMapperX<AgentTaskPO> {
    int pause(@Param("ownerKey") String ownerKey, @Param("taskNo") String taskNo);

    int resume(@Param("ownerKey") String ownerKey, @Param("taskNo") String taskNo);

    int cancel(@Param("ownerKey") String ownerKey, @Param("taskNo") String taskNo);
    int linkRun(@Param("ownerKey") String ownerKey, @Param("taskNo") String taskNo,
                @Param("leaseOwner") String leaseOwner, @Param("runNo") String runNo);

    List<AgentTaskPO> claimDue(@Param("workerId") String workerId, @Param("now") OffsetDateTime now,
                               @Param("leaseUntil") OffsetDateTime leaseUntil, @Param("limit") int limit);

    int complete(AgentTaskPO task);

    int retry(AgentTaskPO task);

    int deadLetter(AgentTaskPO task);

    @Update("UPDATE cynexus_appl_ai.ai_agent_task SET status='ARCHIVED', user_input='[ARCHIVED_BY_RETENTION_POLICY]', "
            + "preferred_model_code=NULL, last_error=NULL, completed_at=COALESCE(completed_at, now()), updated_at=now() "
            + "WHERE updated_at < #{cutoff} AND status IN ('COMPLETED','DEAD_LETTER','CANCELLED') AND deleted_at IS NULL")
    int archiveBefore(@Param("cutoff") OffsetDateTime cutoff);
}
