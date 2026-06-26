package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import java.util.Optional;

public interface AiScenarioRepository {

    Optional<AiScenarioBO> findByCode(String scenarioCode);
}
