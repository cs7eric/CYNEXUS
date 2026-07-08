package cn.cccs7.cynexus.core.infra.basic.service;

import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import cn.cccs7.cynexus.core.infra.basic.entity.DataSourceHealthCheckItemPO;
import java.util.List;
import java.util.Map;

public interface DataSourceHealthCheckItemBasicService extends CrudBasicService<DataSourceHealthCheckItemPO> {

    List<DataSourceHealthCheckItemPO> listByRunId(String runId);

    List<Map<String, Object>> successRate24h();
}
