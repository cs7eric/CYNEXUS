package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MsiIndexDailyBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MsiIndexDailyPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MsiIndexDailyPOConverter extends BaseMapper<MsiIndexDailyPO, MsiIndexDailyBO> {

    MsiIndexDailyPOConverter INSTANCE = Mappers.getMapper(MsiIndexDailyPOConverter.class);
}
