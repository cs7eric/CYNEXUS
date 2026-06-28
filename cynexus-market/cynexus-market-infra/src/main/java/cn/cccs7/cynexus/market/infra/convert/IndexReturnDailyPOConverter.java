package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.IndexReturnDailyBO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndexReturnDailyPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface IndexReturnDailyPOConverter extends BaseMapper<IndexReturnDailyPO, IndexReturnDailyBO> {

    IndexReturnDailyPOConverter INSTANCE = Mappers.getMapper(IndexReturnDailyPOConverter.class);
}
