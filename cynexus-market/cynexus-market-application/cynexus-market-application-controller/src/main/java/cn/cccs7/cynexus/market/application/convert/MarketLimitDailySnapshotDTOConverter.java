package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.MarketLimitDailySnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarketLimitDailySnapshotDTOConverter
        extends BaseMapper<MarketLimitDailySnapshotDTO, MarketLimitDailySnapshotBO> {

    MarketLimitDailySnapshotDTOConverter INSTANCE = Mappers.getMapper(MarketLimitDailySnapshotDTOConverter.class);
}
