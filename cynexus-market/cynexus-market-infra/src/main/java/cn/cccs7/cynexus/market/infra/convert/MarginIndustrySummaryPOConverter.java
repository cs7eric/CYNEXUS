package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustrySummaryBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustrySummaryPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarginIndustrySummaryPOConverter extends BaseMapper<MarginIndustrySummaryPO, MarginIndustrySummaryBO> {

    MarginIndustrySummaryPOConverter INSTANCE = Mappers.getMapper(MarginIndustrySummaryPOConverter.class);
}
