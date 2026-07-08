package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;

public interface AiExecutionLogRepository {

    Long create(AiExecutionLogBO log);
}
