package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.entity.DataSourceHealthCheckItemBO;
import cn.cccs7.cynexus.core.domain.entity.DataSourceHealthCheckRunBO;
import cn.cccs7.cynexus.core.domain.repository.DataSourceHealthRepository;
import cn.cccs7.cynexus.core.infra.basic.service.DataSourceHealthCheckItemBasicService;
import cn.cccs7.cynexus.core.infra.basic.service.DataSourceHealthCheckRunBasicService;
import cn.cccs7.cynexus.core.infra.convert.DataSourceHealthCheckItemPOConverter;
import cn.cccs7.cynexus.core.infra.convert.DataSourceHealthCheckRunPOConverter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class DataSourceHealthRepositoryImpl implements DataSourceHealthRepository {

    private final DataSourceHealthCheckRunBasicService runBasicService;
    private final DataSourceHealthCheckItemBasicService itemBasicService;

    public DataSourceHealthRepositoryImpl(DataSourceHealthCheckRunBasicService runBasicService,
                                          DataSourceHealthCheckItemBasicService itemBasicService) {
        this.runBasicService = runBasicService;
        this.itemBasicService = itemBasicService;
    }

    @Override
    public Long createRun(DataSourceHealthCheckRunBO run) {
        return runBasicService.create(DataSourceHealthCheckRunPOConverter.INSTANCE.toSource(run));
    }

    @Override
    public Long createItem(DataSourceHealthCheckItemBO item) {
        return itemBasicService.create(DataSourceHealthCheckItemPOConverter.INSTANCE.toSource(item));
    }

    @Override
    public DataSourceHealthCheckRunBO latestRun() {
        return DataSourceHealthCheckRunPOConverter.INSTANCE.toTarget(runBasicService.latest());
    }

    @Override
    public List<DataSourceHealthCheckItemBO> listItemsByRunId(String runId) {
        return DataSourceHealthCheckItemPOConverter.INSTANCE.toTargetList(itemBasicService.listByRunId(runId));
    }

    @Override
    public Map<String, Double> successRate24hBySource() {
        Map<String, Double> result = new HashMap<>();
        for (Map<String, Object> row : itemBasicService.successRate24h()) {
            Object sourceCode = row.get("sourcecode");
            if (sourceCode == null) {
                sourceCode = row.get("sourceCode");
            }
            Object successRate = row.get("successrate");
            if (successRate == null) {
                successRate = row.get("successRate");
            }
            if (sourceCode != null && successRate != null) {
                result.put(String.valueOf(sourceCode), Double.parseDouble(String.valueOf(successRate)));
            }
        }
        return result;
    }
}
