package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AiModelConfigPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AiModelConfigMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.AiModelConfigBasicService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiModelConfigBasicServiceImpl implements AiModelConfigBasicService {

    private final AiModelConfigMapper aiModelConfigMapper;

    public AiModelConfigBasicServiceImpl(AiModelConfigMapper aiModelConfigMapper) {
        this.aiModelConfigMapper = aiModelConfigMapper;
    }

    @Override
    public List<AiModelConfigPO> listEnabled() {
        return aiModelConfigMapper.selectList(null);
    }
}
