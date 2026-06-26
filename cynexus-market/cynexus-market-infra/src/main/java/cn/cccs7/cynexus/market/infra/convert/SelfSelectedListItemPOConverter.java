package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListItemBO;
import cn.cccs7.cynexus.market.infra.basic.entity.SelfSelectedListItemPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SelfSelectedListItemPOConverter extends BaseMapper<SelfSelectedListItemPO, SelfSelectedListItemBO> {

    SelfSelectedListItemPOConverter INSTANCE = Mappers.getMapper(SelfSelectedListItemPOConverter.class);
}
