package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxThsIndustryMappingPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.TdxThsIndustryMappingMapper;
import cn.cccs7.cynexus.market.infra.basic.service.TdxThsIndustryMappingBasicService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TdxThsIndustryMappingBasicServiceImpl extends AbstractCrudBasicService<TdxThsIndustryMappingPO>
        implements TdxThsIndustryMappingBasicService {

    private final TdxThsIndustryMappingMapper tdxThsIndustryMappingMapper;

    public TdxThsIndustryMappingBasicServiceImpl(TdxThsIndustryMappingMapper mapper,
                                                 SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.tdxThsIndustryMappingMapper = mapper;
    }

    @Override
    public List<TdxThsIndustryMappingPO> listByTdxCode6(String tdxCode6) {
        return tdxThsIndustryMappingMapper.selectByTdxCode6(tdxCode6);
    }
}
