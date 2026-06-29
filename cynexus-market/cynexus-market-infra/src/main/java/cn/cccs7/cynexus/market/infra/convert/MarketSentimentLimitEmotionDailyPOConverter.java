package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MarketSentimentLimitEmotionDailyBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketSentimentLimitEmotionDailyPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarketSentimentLimitEmotionDailyPOConverter extends BaseMapper<MarketSentimentLimitEmotionDailyPO, MarketSentimentLimitEmotionDailyBO> {

    MarketSentimentLimitEmotionDailyPOConverter INSTANCE = Mappers.getMapper(MarketSentimentLimitEmotionDailyPOConverter.class);
}
