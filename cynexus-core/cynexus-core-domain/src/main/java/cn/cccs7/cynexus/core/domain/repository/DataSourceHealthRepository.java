package cn.cccs7.cynexus.core.domain.repository;

import cn.cccs7.cynexus.core.domain.entity.DataSourceHealthCheckItemBO;
import cn.cccs7.cynexus.core.domain.entity.DataSourceHealthCheckRunBO;
import java.util.List;
import java.util.Map;

public interface DataSourceHealthRepository {

    Long createRun(DataSourceHealthCheckRunBO run);

    Long createItem(DataSourceHealthCheckItemBO item);

    DataSourceHealthCheckRunBO latestRun();

    List<DataSourceHealthCheckItemBO> listItemsByRunId(String runId);

    Map<String, Double> successRate24hBySource();
}
