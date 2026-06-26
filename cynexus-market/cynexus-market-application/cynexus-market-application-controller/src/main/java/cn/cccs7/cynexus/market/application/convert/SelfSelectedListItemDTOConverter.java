package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.SelfSelectedListItemDTO;
import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListItemBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SelfSelectedListItemDTOConverter extends BaseMapper<SelfSelectedListItemDTO, SelfSelectedListItemBO> {

    SelfSelectedListItemDTOConverter INSTANCE = Mappers.getMapper(SelfSelectedListItemDTOConverter.class);
}
