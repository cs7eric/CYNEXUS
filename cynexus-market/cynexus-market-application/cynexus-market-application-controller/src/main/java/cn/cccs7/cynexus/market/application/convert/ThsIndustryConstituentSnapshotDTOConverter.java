package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.ThsIndustryConstituentSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentSnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ThsIndustryConstituentSnapshotDTOConverter extends BaseMapper<ThsIndustryConstituentSnapshotDTO, ThsIndustryConstituentSnapshotBO> {

    ThsIndustryConstituentSnapshotDTOConverter INSTANCE = Mappers.getMapper(ThsIndustryConstituentSnapshotDTOConverter.class);
}
