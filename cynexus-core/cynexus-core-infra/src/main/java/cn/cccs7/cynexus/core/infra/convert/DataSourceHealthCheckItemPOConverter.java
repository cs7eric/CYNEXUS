package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.DataSourceHealthCheckItemBO;
import cn.cccs7.cynexus.core.infra.basic.entity.DataSourceHealthCheckItemPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataSourceHealthCheckItemPOConverter
        extends BaseMapper<DataSourceHealthCheckItemPO, DataSourceHealthCheckItemBO> {

    DataSourceHealthCheckItemPOConverter INSTANCE = Mappers.getMapper(DataSourceHealthCheckItemPOConverter.class);
}
