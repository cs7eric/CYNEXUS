package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.AiExecutionLogBasicService;
import cn.cccs7.cynexus.ai.infra.convert.AiExecutionLogPOConverter;
import org.springframework.stereotype.Repository;

@Repository
public class AiExecutionLogRepositoryImpl implements AiExecutionLogRepository {

    private final AiExecutionLogBasicService aiExecutionLogBasicService;

    public AiExecutionLogRepositoryImpl(AiExecutionLogBasicService aiExecutionLogBasicService) {
        this.aiExecutionLogBasicService = aiExecutionLogBasicService;
    }

    @Override
    public Long create(AiExecutionLogBO log) {
        return aiExecutionLogBasicService.create(AiExecutionLogPOConverter.INSTANCE.toSource(log));
    }
}
