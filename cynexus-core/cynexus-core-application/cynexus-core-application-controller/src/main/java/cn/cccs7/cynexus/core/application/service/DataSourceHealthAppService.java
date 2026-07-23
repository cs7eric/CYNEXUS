package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.dto.DataSourceHealthDTO;
import cn.cccs7.cynexus.core.domain.entity.DataSourceHealthCheckItemBO;
import cn.cccs7.cynexus.core.domain.service.datasource.DataSourceHealthDomainService;
import cn.cccs7.cynexus.core.domain.service.datasource.DataSourceHealthDomainService.DataSourceHealthResult;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DataSourceHealthAppService {

    private final DataSourceHealthDomainService domainService;

    public DataSourceHealthAppService(DataSourceHealthDomainService domainService) {
        this.domainService = domainService;
    }

    public DataSourceHealthDTO runSnapshot(boolean includeSlow) {
        return toDto(domainService.runSnapshot(includeSlow));
    }

    public DataSourceHealthDTO latestSnapshot() {
        return toDto(domainService.latestSnapshot());
    }

    private DataSourceHealthDTO toDto(DataSourceHealthResult result) {
        List<DataSourceHealthDTO.Item> items = result.items().stream()
                .map(item -> toItem(item, result.successRates()))
                .toList();
        var run = result.run();
        Map<String, Object> payload = result.payload();
        return DataSourceHealthDTO.builder()
                .ok(run.getFailedCount() == null || run.getFailedCount() == 0)
                .generatedAt(asOffsetDateTime(payload.get("generated_at"), run.getFinishedAt()))
                .durationMs(run.getDurationMs())
                .includeSlow(run.getIncludeSlow())
                .summary(DataSourceHealthDTO.Summary.builder()
                        .total(run.getTotalCount()).healthy(run.getHealthyCount()).degraded(run.getDegradedCount())
                        .failed(run.getFailedCount()).skipped(run.getSkippedCount()).successRate(run.getSuccessRate())
                        .avgLatencyMs(run.getAvgLatencyMs()).build())
                .run(DataSourceHealthDTO.Run.builder().runId(run.getRunId()).triggerType(run.getTriggerType())
                        .status(run.getStatus()).startedAt(run.getStartedAt()).finishedAt(run.getFinishedAt())
                        .durationMs(run.getDurationMs()).build())
                .items(items)
                .build();
    }

    private DataSourceHealthDTO.Item toItem(DataSourceHealthCheckItemBO item, Map<String, Double> successRates) {
        return DataSourceHealthDTO.Item.builder()
                .sourceCode(item.getSourceCode()).sourceName(item.getSourceName()).sourceType(item.getSourceType())
                .category(item.getCategory()).provider(item.getProvider()).description(item.getDescription())
                .endpoint(item.getEndpoint()).status(item.getStatus()).connectivity(item.getConnectivity())
                .authenticityStatus(item.getAuthenticityStatus()).latencyMs(item.getLatencyMs())
                .sampleCount(item.getSampleCount())
                .successRate24h(successRates.containsKey(item.getSourceCode())
                        ? BigDecimal.valueOf(successRates.get(item.getSourceCode())) : item.getSuccessRate24h())
                .checkedAt(item.getCheckedAt()).errorMessage(item.getErrorMessage()).checks(item.getChecks())
                .rawPayload(item.getRawPayload()).build();
    }

    private static OffsetDateTime asOffsetDateTime(Object value, OffsetDateTime defaultValue) {
        if (value == null) return defaultValue;
        try { return OffsetDateTime.parse(String.valueOf(value)); }
        catch (Exception exception) { return defaultValue; }
    }
}
