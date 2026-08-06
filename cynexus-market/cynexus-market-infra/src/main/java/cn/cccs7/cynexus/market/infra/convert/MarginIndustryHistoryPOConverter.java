package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustryHistoryBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustryHistoryPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarginIndustryHistoryPOConverter extends BaseMapper<MarginIndustryHistoryPO, MarginIndustryHistoryBO> {

    MarginIndustryHistoryPOConverter INSTANCE = Mappers.getMapper(MarginIndustryHistoryPOConverter.class);
}
