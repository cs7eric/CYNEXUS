package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentCaptureBatchBO;
import cn.cccs7.cynexus.market.infra.basic.entity.ThsIndustryConstituentCaptureBatchPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ThsIndustryConstituentCaptureBatchPOConverter extends BaseMapper<ThsIndustryConstituentCaptureBatchPO, ThsIndustryConstituentCaptureBatchBO> {

    ThsIndustryConstituentCaptureBatchPOConverter INSTANCE = Mappers.getMapper(ThsIndustryConstituentCaptureBatchPOConverter.class);
}
