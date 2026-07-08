package cn.cccs7.cynexus.core.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.core.domain.entity.DataSourceHealthCheckRunBO;
import cn.cccs7.cynexus.core.infra.basic.entity.DataSourceHealthCheckRunPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataSourceHealthCheckRunPOConverter
        extends BaseMapper<DataSourceHealthCheckRunPO, DataSourceHealthCheckRunBO> {

    DataSourceHealthCheckRunPOConverter INSTANCE = Mappers.getMapper(DataSourceHealthCheckRunPOConverter.class);
}
