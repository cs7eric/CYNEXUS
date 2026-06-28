package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.MarketPulseLimitEmotionDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MarketPulseLimitEmotionStockDTOConverter
        extends BaseMapper<MarketPulseLimitEmotionDTO.StockDTO, MarketLimitDailyStockBO> {

    MarketPulseLimitEmotionStockDTOConverter INSTANCE = Mappers.getMapper(MarketPulseLimitEmotionStockDTOConverter.class);
}
