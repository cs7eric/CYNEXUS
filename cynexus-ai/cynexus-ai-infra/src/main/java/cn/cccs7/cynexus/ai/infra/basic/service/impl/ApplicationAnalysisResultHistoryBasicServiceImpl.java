package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisResultHistoryPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.ApplicationAnalysisResultHistoryMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisResultHistoryBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAnalysisResultHistoryBasicServiceImpl
        extends AbstractCrudBasicService<ApplicationAnalysisResultHistoryPO>
        implements ApplicationAnalysisResultHistoryBasicService {

    private final ApplicationAnalysisResultHistoryMapper mapper;

    public ApplicationAnalysisResultHistoryBasicServiceImpl(ApplicationAnalysisResultHistoryMapper mapper,
                                                            SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public List<ApplicationAnalysisResultHistoryPO> listActiveByTargetKey(String targetKey, int limit) {
        return mapper.selectActiveByTargetKey(targetKey, limit);
    }
}
