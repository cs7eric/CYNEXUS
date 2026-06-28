package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentSnapshotBO;
import cn.cccs7.cynexus.market.infra.basic.entity.ThsIndustryConstituentSnapshotPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ThsIndustryConstituentSnapshotPOConverter extends BaseMapper<ThsIndustryConstituentSnapshotPO, ThsIndustryConstituentSnapshotBO> {

    ThsIndustryConstituentSnapshotPOConverter INSTANCE = Mappers.getMapper(ThsIndustryConstituentSnapshotPOConverter.class);
}
