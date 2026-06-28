package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.ThsIndustryConstituentCaptureBatchDTO;
import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentCaptureBatchBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ThsIndustryConstituentCaptureBatchDTOConverter extends BaseMapper<ThsIndustryConstituentCaptureBatchDTO, ThsIndustryConstituentCaptureBatchBO> {

    ThsIndustryConstituentCaptureBatchDTOConverter INSTANCE = Mappers.getMapper(ThsIndustryConstituentCaptureBatchDTOConverter.class);
}
