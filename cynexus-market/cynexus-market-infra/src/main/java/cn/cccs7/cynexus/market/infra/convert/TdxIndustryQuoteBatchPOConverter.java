package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteBatchBO;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxIndustryQuoteBatchPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TdxIndustryQuoteBatchPOConverter extends BaseMapper<TdxIndustryQuoteBatchPO, TdxIndustryQuoteBatchBO> {

    TdxIndustryQuoteBatchPOConverter INSTANCE = Mappers.getMapper(TdxIndustryQuoteBatchPOConverter.class);
}
