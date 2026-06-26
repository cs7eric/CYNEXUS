package cn.cccs7.cynexus.core.application.convert;

import cn.cccs7.cynexus.core.application.dto.CoreServiceInfoDTO;
import cn.cccs7.cynexus.core.domain.entity.CoreServiceInfoBO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CoreServiceInfoDTOConverter extends BaseMapper<CoreServiceInfoDTO, CoreServiceInfoBO> {

    CoreServiceInfoDTOConverter INSTANCE = Mappers.getMapper(CoreServiceInfoDTOConverter.class);
}
