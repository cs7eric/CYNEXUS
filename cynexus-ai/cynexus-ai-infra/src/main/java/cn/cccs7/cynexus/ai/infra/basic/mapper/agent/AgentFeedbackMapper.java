package cn.cccs7.cynexus.ai.infra.basic.mapper.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentFeedbackPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentFeedbackSummaryPO;
import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentFeedbackMapper extends BaseMapperX<AgentFeedbackPO> {
    @Insert("INSERT INTO cynexus_appl_ai.ai_agent_feedback "
            + "(id, owner_key, tenant_key, principal_key, run_no, conversation_no, rating, feedback_reason, correction_text, evaluation_label, created_at, updated_at) "
            + "VALUES (#{id}, #{ownerKey}, #{tenantKey}, #{principalKey}, #{runNo}, #{conversationNo}, #{rating}, #{feedbackReason}, #{correctionText}, #{evaluationLabel}, COALESCE(#{createdAt}, now()), now()) "
            + "ON CONFLICT (owner_key, run_no) DO UPDATE SET rating=EXCLUDED.rating, feedback_reason=EXCLUDED.feedback_reason, "
            + "correction_text=EXCLUDED.correction_text, evaluation_label=EXCLUDED.evaluation_label, updated_at=now()")
    int upsert(AgentFeedbackPO feedback);

    @Select("SELECT id, owner_key, tenant_key, principal_key, run_no, conversation_no, rating, feedback_reason, correction_text, evaluation_label, reviewer_key, reviewer_note, reviewed_at, created_at, updated_at "
            + "FROM cynexus_appl_ai.ai_agent_feedback WHERE owner_key=#{ownerKey} AND run_no=#{runNo} AND deleted_at IS NULL LIMIT 1")
    AgentFeedbackPO find(String ownerKey, String runNo);

    @Select("SELECT count(*) AS total, COALESCE(sum(CASE WHEN rating >= 4 THEN 1 ELSE 0 END), 0) AS positive, "
            + "COALESCE(sum(CASE WHEN rating <= 2 THEN 1 ELSE 0 END), 0) AS negative, "
            + "COALESCE(sum(CASE WHEN reviewed_at IS NOT NULL THEN 1 ELSE 0 END), 0) AS reviewed, "
            + "COALESCE(avg(rating), 0) AS average_rating FROM cynexus_appl_ai.ai_agent_feedback "
            + "WHERE owner_key=#{ownerKey} AND deleted_at IS NULL")
    AgentFeedbackSummaryPO summarize(String ownerKey);

    @Update("UPDATE cynexus_appl_ai.ai_agent_feedback SET deleted_at=now(), updated_at=now() WHERE created_at < #{cutoff} AND deleted_at IS NULL")
    int archiveBefore(java.time.OffsetDateTime cutoff);
}
