package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MaCountDailyBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MaCountDailyPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MaCountDailyPOConverter extends BaseMapper<MaCountDailyPO, MaCountDailyBO> {

    MaCountDailyPOConverter INSTANCE = Mappers.getMapper(MaCountDailyPOConverter.class);
}
