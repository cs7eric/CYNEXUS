package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketLimitDailySnapshotPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarketLimitDailySnapshotPOConverter extends BaseMapper<MarketLimitDailySnapshotPO, MarketLimitDailySnapshotBO> {

    MarketLimitDailySnapshotPOConverter INSTANCE = Mappers.getMapper(MarketLimitDailySnapshotPOConverter.class);
}
