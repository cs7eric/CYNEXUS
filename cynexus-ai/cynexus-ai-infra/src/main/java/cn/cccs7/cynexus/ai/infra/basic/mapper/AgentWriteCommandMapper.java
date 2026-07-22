package cn.cccs7.cynexus.ai.infra.basic.mapper;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentWriteCommandPO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentWriteCommandMapper extends BaseMapperX<AgentWriteCommandPO> {
    @Select("SELECT * FROM cynexus_appl_ai.ai_agent_write_command WHERE owner_key=#{ownerKey} AND command_no=#{commandNo} "
            + "AND status='PENDING_CONFIRMATION' AND deleted_at IS NULL LIMIT 1")
    AgentWriteCommandPO findPending(@Param("ownerKey") String ownerKey, @Param("commandNo") String commandNo);

    @Update("UPDATE cynexus_appl_ai.ai_agent_write_command SET status='CONFIRMED', confirmed_at=now(), updated_at=now() "
            + "WHERE owner_key=#{ownerKey} AND command_no=#{commandNo} AND confirmation_token_hash=#{tokenHash} "
            + "AND status='PENDING_CONFIRMATION' AND expires_at > #{now} AND deleted_at IS NULL")
    int markConfirmed(@Param("ownerKey") String ownerKey, @Param("commandNo") String commandNo,
                      @Param("tokenHash") String tokenHash, @Param("now") OffsetDateTime now);

    @Update("UPDATE cynexus_appl_ai.ai_agent_write_command SET status=#{status}, executed_at=#{executedAt}, result_json=#{resultJson,typeHandler=cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler}, "
            + "error_message=#{errorMessage}, updated_at=now() WHERE owner_key=#{ownerKey} AND command_no=#{commandNo} AND status='CONFIRMED'")
    int markExecuted(AgentWriteCommandPO command);

    @Update("UPDATE cynexus_appl_ai.ai_agent_write_command SET status='REJECTED', updated_at=now() "
            + "WHERE owner_key=#{ownerKey} AND command_no=#{commandNo} AND status='PENDING_CONFIRMATION'")
    int markRejected(AgentWriteCommandPO command);
}
