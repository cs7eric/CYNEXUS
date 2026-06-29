package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisResultCurrentPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.ApplicationAnalysisResultCurrentMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisResultCurrentBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAnalysisResultCurrentBasicServiceImpl
        extends AbstractCrudBasicService<ApplicationAnalysisResultCurrentPO>
        implements ApplicationAnalysisResultCurrentBasicService {

    private final ApplicationAnalysisResultCurrentMapper mapper;

    public ApplicationAnalysisResultCurrentBasicServiceImpl(ApplicationAnalysisResultCurrentMapper mapper,
                                                            SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public ApplicationAnalysisResultCurrentPO getActiveByTargetKey(String targetKey) {
        return mapper.selectActiveByTargetKey(targetKey);
    }
}
