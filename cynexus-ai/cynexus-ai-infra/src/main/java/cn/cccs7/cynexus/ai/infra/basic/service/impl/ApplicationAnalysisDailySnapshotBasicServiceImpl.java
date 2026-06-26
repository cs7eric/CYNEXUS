package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisDailySnapshotPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.ApplicationAnalysisDailySnapshotMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisDailySnapshotBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAnalysisDailySnapshotBasicServiceImpl
        extends AbstractCrudBasicService<ApplicationAnalysisDailySnapshotPO>
        implements ApplicationAnalysisDailySnapshotBasicService {

    public ApplicationAnalysisDailySnapshotBasicServiceImpl(ApplicationAnalysisDailySnapshotMapper mapper,
                                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
    }
}
