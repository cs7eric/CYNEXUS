package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.DataSourceHealthCheckItemPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.DataSourceHealthCheckItemMapper;
import cn.cccs7.cynexus.core.infra.basic.service.DataSourceHealthCheckItemBasicService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DataSourceHealthCheckItemBasicServiceImpl
        extends AbstractCrudBasicService<DataSourceHealthCheckItemPO>
        implements DataSourceHealthCheckItemBasicService {

    private final DataSourceHealthCheckItemMapper mapper;

    public DataSourceHealthCheckItemBasicServiceImpl(DataSourceHealthCheckItemMapper mapper,
                                                     SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public List<DataSourceHealthCheckItemPO> listByRunId(String runId) {
        return mapper.selectByRunId(runId);
    }

    @Override
    public List<Map<String, Object>> successRate24h() {
        return mapper.selectSuccessRate24h();
    }
}
