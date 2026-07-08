package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.ProviderCapabilityBO;
import cn.cccs7.cynexus.core.infra.basic.entity.ProviderCapabilityPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProviderCapabilityPOConverter extends BaseMapper<ProviderCapabilityPO, ProviderCapabilityBO> {

    ProviderCapabilityPOConverter INSTANCE = Mappers.getMapper(ProviderCapabilityPOConverter.class);
}
