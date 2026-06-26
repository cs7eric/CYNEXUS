package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.MarketPulseSectorDailySnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarketPulseSectorDailySnapshotDTOConverter
        extends BaseMapper<MarketPulseSectorDailySnapshotDTO, MarketPulseSectorDailySnapshotBO> {

    MarketPulseSectorDailySnapshotDTOConverter INSTANCE =
            Mappers.getMapper(MarketPulseSectorDailySnapshotDTOConverter.class);
}
