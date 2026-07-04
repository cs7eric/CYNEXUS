package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.ThsIndustryConstituentSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.ThsIndustryConstituentSnapshotMapper;
import cn.cccs7.cynexus.market.infra.basic.service.ThsIndustryConstituentSnapshotBasicService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ThsIndustryConstituentSnapshotBasicServiceImpl extends AbstractCrudBasicService<ThsIndustryConstituentSnapshotPO>
        implements ThsIndustryConstituentSnapshotBasicService {

    private final ThsIndustryConstituentSnapshotMapper thsIndustryConstituentSnapshotMapper;

    public ThsIndustryConstituentSnapshotBasicServiceImpl(ThsIndustryConstituentSnapshotMapper mapper,
                                           SnowflakeIdGenerator snowflakeIdGenerator) {
        super(mapper, snowflakeIdGenerator);
        this.thsIndustryConstituentSnapshotMapper = mapper;
    }

    @Override
    public List<ThsIndustryConstituentSnapshotPO> listLatestByIndustryCode(String industryCode) {
        return thsIndustryConstituentSnapshotMapper.selectLatestByIndustryCode(industryCode);
    }

    @Override
    public List<ThsIndustryConstituentSnapshotPO> listLatestByIndustryCodeOnOrBefore(String industryCode, LocalDate snapshotDate) {
        return thsIndustryConstituentSnapshotMapper.selectLatestByIndustryCodeOnOrBefore(industryCode, snapshotDate);
    }
}
