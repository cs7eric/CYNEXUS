package cn.cccs7.cynexus.ai.domain.service.model;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import java.util.List;

/**
 * Domain entry point for the enabled AI model query.
 */
public interface AiModelQueryDomainService {

    List<AiModelBO> findEnabledModels();
}
