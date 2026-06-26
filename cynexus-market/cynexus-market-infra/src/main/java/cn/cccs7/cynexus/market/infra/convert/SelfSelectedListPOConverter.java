package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListBO;
import cn.cccs7.cynexus.market.infra.basic.entity.SelfSelectedListPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SelfSelectedListPOConverter extends BaseMapper<SelfSelectedListPO, SelfSelectedListBO> {

    SelfSelectedListPOConverter INSTANCE = Mappers.getMapper(SelfSelectedListPOConverter.class);
}
