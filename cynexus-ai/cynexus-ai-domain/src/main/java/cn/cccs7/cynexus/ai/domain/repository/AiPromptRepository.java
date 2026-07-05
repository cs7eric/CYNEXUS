package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import java.util.List;

public interface AiPromptRepository {

    List<AiPromptBO> list(String keyword, String scenarioCode, String status);

    AiPromptBO findById(Long id);

    Long create(AiPromptBO prompt);

    boolean update(AiPromptBO prompt);

    boolean delete(Long id);
}
