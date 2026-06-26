package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisTargetPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.ApplicationAnalysisTargetMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisTargetBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAnalysisTargetBasicServiceImpl extends AbstractCrudBasicService<ApplicationAnalysisTargetPO>
        implements ApplicationAnalysisTargetBasicService {

    public ApplicationAnalysisTargetBasicServiceImpl(ApplicationAnalysisTargetMapper mapper,
                                                     SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
