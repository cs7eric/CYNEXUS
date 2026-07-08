package cn.cccs7.cynexus.core.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.ProviderCapabilityPO;
import cn.cccs7.cynexus.core.infra.basic.mapper.ProviderCapabilityMapper;
import cn.cccs7.cynexus.core.infra.basic.service.ProviderCapabilityBasicService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProviderCapabilityBasicServiceImpl
        extends AbstractCrudBasicService<ProviderCapabilityPO>
        implements ProviderCapabilityBasicService {

    private final ProviderCapabilityMapper mapper;

    public ProviderCapabilityBasicServiceImpl(ProviderCapabilityMapper mapper,
                                             SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.mapper = mapper;
    }

    @Override
    public List<ProviderCapabilityPO> listEnabled() {
        return mapper.selectEnabled();
    }
}
