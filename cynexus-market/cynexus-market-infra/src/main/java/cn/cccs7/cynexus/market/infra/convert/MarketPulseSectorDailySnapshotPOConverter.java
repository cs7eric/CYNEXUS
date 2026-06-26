package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketPulseSectorDailySnapshotPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarketPulseSectorDailySnapshotPOConverter
        extends BaseMapper<MarketPulseSectorDailySnapshotPO, MarketPulseSectorDailySnapshotBO> {

    MarketPulseSectorDailySnapshotPOConverter INSTANCE =
            Mappers.getMapper(MarketPulseSectorDailySnapshotPOConverter.class);
}
