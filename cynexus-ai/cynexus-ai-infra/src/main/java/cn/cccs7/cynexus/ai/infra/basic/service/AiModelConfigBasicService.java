package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.AiModelConfigPO;
import java.util.List;

public interface AiModelConfigBasicService {

    List<AiModelConfigPO> listEnabled();
}
