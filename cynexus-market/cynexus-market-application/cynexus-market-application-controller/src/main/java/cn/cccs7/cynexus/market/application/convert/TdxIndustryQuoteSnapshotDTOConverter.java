package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.TdxIndustryQuoteSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteSnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TdxIndustryQuoteSnapshotDTOConverter extends BaseMapper<TdxIndustryQuoteSnapshotDTO, TdxIndustryQuoteSnapshotBO> {

    TdxIndustryQuoteSnapshotDTOConverter INSTANCE = Mappers.getMapper(TdxIndustryQuoteSnapshotDTOConverter.class);
}
