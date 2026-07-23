package cn.cccs7.cynexus.core.domain.service.datasource;

import cn.cccs7.cynexus.core.domain.entity.DataSourceHealthCheckItemBO;
import cn.cccs7.cynexus.core.domain.entity.DataSourceHealthCheckRunBO;
import java.util.List;
import java.util.Map;

public interface DataSourceHealthDomainService {

    DataSourceHealthResult runSnapshot(boolean includeSlow);

    DataSourceHealthResult latestSnapshot();

    record DataSourceHealthResult(Map<String, Object> payload,
                                  DataSourceHealthCheckRunBO run,
                                  List<DataSourceHealthCheckItemBO> items,
                                  Map<String, Double> successRates) {
    }
}
