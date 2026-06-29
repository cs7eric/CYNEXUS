package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.IndexDailyBarBO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndexDailyBarPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface IndexDailyBarPOConverter extends BaseMapper<IndexDailyBarPO, IndexDailyBarBO> {

    IndexDailyBarPOConverter INSTANCE = Mappers.getMapper(IndexDailyBarPOConverter.class);
}
