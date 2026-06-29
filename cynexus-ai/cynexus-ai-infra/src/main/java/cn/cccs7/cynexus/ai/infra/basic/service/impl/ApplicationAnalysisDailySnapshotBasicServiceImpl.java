package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisDailySnapshotPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.ApplicationAnalysisDailySnapshotMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.ApplicationAnalysisDailySnapshotBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAnalysisDailySnapshotBasicServiceImpl
        extends AbstractCrudBasicService<ApplicationAnalysisDailySnapshotPO>
        implements ApplicationAnalysisDailySnapshotBasicService {

    private final ApplicationAnalysisDailySnapshotMapper mapper;

    public ApplicationAnalysisDailySnapshotBasicServiceImpl(ApplicationAnalysisDailySnapshotMapper mapper,
                                                            SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public List<ApplicationAnalysisDailySnapshotPO> listRecent30ByTargetKey(String targetKey, int limit) {
        return mapper.selectRecent30ByTargetKey(targetKey, limit);
    }

    @Override
    public ApplicationAnalysisDailySnapshotPO getRecent30ByTargetKeyAndDate(String targetKey, LocalDate tradeDate) {
        return mapper.selectRecent30ByTargetKeyAndDate(targetKey, tradeDate);
    }
}
