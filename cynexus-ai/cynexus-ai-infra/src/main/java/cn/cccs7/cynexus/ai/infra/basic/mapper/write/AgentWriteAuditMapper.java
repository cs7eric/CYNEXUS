package cn.cccs7.cynexus.ai.infra.basic.mapper.write;

import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentWriteAuditMapper {
    @Insert("INSERT INTO cynexus_appl_ai.ai_agent_write_audit (command_no, owner_key, action, status, tool_code, argument_digest, result_json, error_message, occurred_at) "
            + "VALUES (#{commandNo}, #{ownerKey}, #{action}, #{status}, #{toolCode}, #{argumentDigest}, #{resultJson,typeHandler=cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler}, #{errorMessage}, #{occurredAt})")
    int insert(@Param("commandNo") String commandNo, @Param("ownerKey") String ownerKey, @Param("action") String action,
               @Param("status") String status, @Param("toolCode") String toolCode, @Param("argumentDigest") String argumentDigest,
               @Param("resultJson") String resultJson, @Param("errorMessage") String errorMessage, @Param("occurredAt") OffsetDateTime occurredAt);
}
