package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.AiPromptPO;
import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import java.util.List;

public interface AiPromptBasicService extends CrudBasicService<AiPromptPO> {

    List<AiPromptPO> list(String keyword, String scenarioCode, String status);
}
