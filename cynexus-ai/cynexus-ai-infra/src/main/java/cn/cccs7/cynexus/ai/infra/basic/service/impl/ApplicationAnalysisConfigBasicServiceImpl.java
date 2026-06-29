package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisConfigPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.ApplicationAnalysisConfigMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisConfigBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAnalysisConfigBasicServiceImpl extends AbstractCrudBasicService<ApplicationAnalysisConfigPO>
        implements ApplicationAnalysisConfigBasicService {

    private final ApplicationAnalysisConfigMapper mapper;

    public ApplicationAnalysisConfigBasicServiceImpl(ApplicationAnalysisConfigMapper mapper,
                                                     SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public ApplicationAnalysisConfigPO getDefaultActive() {
        return mapper.selectDefaultActive();
    }
}
