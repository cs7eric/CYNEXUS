package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.TdxIndustryQuoteBatchDTO;
import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteBatchBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TdxIndustryQuoteBatchDTOConverter extends BaseMapper<TdxIndustryQuoteBatchDTO, TdxIndustryQuoteBatchBO> {

    TdxIndustryQuoteBatchDTOConverter INSTANCE = Mappers.getMapper(TdxIndustryQuoteBatchDTOConverter.class);
}
