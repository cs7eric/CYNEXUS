package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.DataSourceFreshnessDTO;
import cn.cccs7.cynexus.market.domain.entity.DataSourceFreshnessBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataSourceFreshnessDTOConverter extends BaseMapper<DataSourceFreshnessDTO.ItemDTO, DataSourceFreshnessBO.ItemBO> {

    DataSourceFreshnessDTOConverter INSTANCE = Mappers.getMapper(DataSourceFreshnessDTOConverter.class);
}
