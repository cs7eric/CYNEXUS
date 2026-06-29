package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisTargetPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.ApplicationAnalysisTargetMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisTargetBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAnalysisTargetBasicServiceImpl extends AbstractCrudBasicService<ApplicationAnalysisTargetPO>
        implements ApplicationAnalysisTargetBasicService {

    private final ApplicationAnalysisTargetMapper mapper;

    public ApplicationAnalysisTargetBasicServiceImpl(ApplicationAnalysisTargetMapper mapper,
                                                     SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public List<ApplicationAnalysisTargetPO> listActiveTargets() {
        return mapper.selectActiveTargets();
    }

    @Override
    public ApplicationAnalysisTargetPO getActiveByTargetKey(String targetKey) {
        return mapper.selectActiveByTargetKey(targetKey);
    }

    @Override
    public int softDeleteMissing(List<String> targetKeys, OffsetDateTime deletedAt) {
        return mapper.softDeleteMissing(targetKeys, deletedAt);
    }
}
