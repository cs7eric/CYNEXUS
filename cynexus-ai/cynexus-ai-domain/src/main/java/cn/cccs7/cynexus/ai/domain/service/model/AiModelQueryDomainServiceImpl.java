package cn.cccs7.cynexus.ai.domain.service.model;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import java.util.List;
import java.util.Objects;

/**
 * Applies the domain contract to the model repository result. The repository
 * is expected to query enabled rows, but the domain service keeps that rule
 * explicit at the use-case boundary.
 */
public class AiModelQueryDomainServiceImpl implements AiModelQueryDomainService {

    private final AiModelRepository aiModelRepository;

    public AiModelQueryDomainServiceImpl(AiModelRepository aiModelRepository) {
        this.aiModelRepository = Objects.requireNonNull(aiModelRepository, "aiModelRepository");
    }

    @Override
    public List<AiModelBO> findEnabledModels() {
        List<AiModelBO> models = aiModelRepository.findEnabledModels();
        if (models == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Enabled AI model query returned no result");
        }
        return models.stream()
                .peek(model -> {
                    if (model == null) {
                        throw new BizException(ErrorCode.INTERNAL_ERROR, "Enabled AI model query returned an invalid model");
                    }
                })
                .filter(AiModelBO::isEnabled)
                .toList();
    }
}
