package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.SelfSelectedListDTO;
import cn.cccs7.cynexus.market.domain.entity.SelfSelectedListBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SelfSelectedListDTOConverter extends BaseMapper<SelfSelectedListDTO, SelfSelectedListBO> {

    SelfSelectedListDTOConverter INSTANCE = Mappers.getMapper(SelfSelectedListDTOConverter.class);
}
