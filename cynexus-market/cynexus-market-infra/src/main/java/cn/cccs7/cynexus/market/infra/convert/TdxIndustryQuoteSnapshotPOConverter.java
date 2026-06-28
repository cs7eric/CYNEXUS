package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteSnapshotBO;
import cn.cccs7.cynexus.market.infra.basic.entity.TdxIndustryQuoteSnapshotPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TdxIndustryQuoteSnapshotPOConverter extends BaseMapper<TdxIndustryQuoteSnapshotPO, TdxIndustryQuoteSnapshotBO> {

    TdxIndustryQuoteSnapshotPOConverter INSTANCE = Mappers.getMapper(TdxIndustryQuoteSnapshotPOConverter.class);
}
