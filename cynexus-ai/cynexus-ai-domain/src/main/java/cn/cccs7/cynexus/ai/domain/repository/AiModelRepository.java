package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import java.util.List;

public interface AiModelRepository {

    List<AiModelBO> findEnabledModels();
}
