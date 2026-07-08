package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.DataSourceHealthCheckRunPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.DataSourceHealthCheckRunMapper;
import cn.cccs7.cynexus.core.infra.basic.service.DataSourceHealthCheckRunBasicService;
import org.springframework.stereotype.Service;

@Service
public class DataSourceHealthCheckRunBasicServiceImpl
        extends AbstractCrudBasicService<DataSourceHealthCheckRunPO>
        implements DataSourceHealthCheckRunBasicService {

    private final DataSourceHealthCheckRunMapper mapper;

    public DataSourceHealthCheckRunBasicServiceImpl(DataSourceHealthCheckRunMapper mapper,
                                                    SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public DataSourceHealthCheckRunPO latest() {
        return mapper.selectLatest();
    }
}
