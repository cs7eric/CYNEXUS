package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisResultHistoryPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.ApplicationAnalysisResultHistoryMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisResultHistoryBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAnalysisResultHistoryBasicServiceImpl
        extends AbstractCrudBasicService<ApplicationAnalysisResultHistoryPO>
        implements ApplicationAnalysisResultHistoryBasicService {

    public ApplicationAnalysisResultHistoryBasicServiceImpl(ApplicationAnalysisResultHistoryMapper mapper,
                                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
