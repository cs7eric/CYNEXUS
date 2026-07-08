package cn.cccs7.cynexus.core.infra.basic.mapper;

import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
import cn.cccs7.cynexus.core.infra.basic.entity.DataSourceHealthCheckItemPO;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DataSourceHealthCheckItemMapper extends BaseMapperX<DataSourceHealthCheckItemPO> {

    List<DataSourceHealthCheckItemPO> selectByRunId(@Param("runId") String runId);

    List<Map<String, Object>> selectSuccessRate24h();
}
