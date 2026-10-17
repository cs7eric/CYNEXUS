package cn.cccs7.cynexus.ai.infra.agent.application.analysis;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisPageDomainService;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import cn.cccs7.cynexus.ai.domain.port.ApplicationAnalysisGateway;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter exposing persisted application-analysis data to the
 * Agent tool boundary. It deliberately bypasses the HTTP-facing application
 * DTO service so the Agent runtime does not depend on an application service.
 */
@Component
public class ApplicationAnalysisAgentGatewayImpl implements ApplicationAnalysisGateway {

    private final ApplicationAnalysisPageDomainService pageService;
    private final ObjectMapper objectMapper;

    public ApplicationAnalysisAgentGatewayImpl(ApplicationAnalysisPageDomainService pageService,
                                               ObjectMapper objectMapper) {
        this.pageService = pageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> getResult(String targetId) {
        ApplicationAnalysisPageDomainService.ResultView result = pageService.getResult(targetId);
        if (result == null) return null;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("analysis_input", parseJson(result.analysisInputJson()));
        response.put("analysis_result", parseJson(result.analysisResultJson()));
        response.put("raw_result", parseJson(result.rawResultJson()));
        response.put("_meta_result_path", "postgres://cynexus_appl_ai.application_analysis_result_current/"
                + valueOr(result.targetKey(), ""));
        response.put("_meta_history", result.history() == null ? List.of() : result.history().stream()
                .map(this::historyFile).toList());
        return response;
    }

    @Override
    public Map<String, Object> getRecent30(String targetId, int limit) {
        ApplicationAnalysisPageDomainService.FullView view = pageService.listRecent30Full(targetId, limit);
        if (view == null) return null;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", view.ok());
        response.put("target_id", view.targetId());
        response.put("error", view.error());
        response.put("items", view.items() == null ? List.of() : view.items().stream()
                .map(this::fullItem).toList());
        return response;
    }

    private Map<String, Object> historyFile(ApplicationAnalysisPageDomainService.HistoryFile file) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("filename", file.filename());
        value.put("path", file.path());
        value.put("size_bytes", file.sizeBytes());
        value.put("updated_at", file.updatedAt());
        return value;
    }

    private Map<String, Object> fullItem(ApplicationAnalysisPageDomainService.FullItem item) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("filename", item.filename());
        value.put("path", item.path());
        value.put("date", item.date());
        value.put("size_bytes", item.sizeBytes());
        value.put("updated_at", item.updatedAt());
        value.put("snapshot", snapshot(item.snapshot()));
        return value;
    }

    private Map<String, Object> snapshot(ApplicationAnalysisPageDomainService.SnapshotDetail detail) {
        if (detail == null) return null;
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("target", target(detail.target(), detail.tags()));
        value.put("date", detail.snapshot() == null ? null : detail.snapshot().getTradeDate());
        value.put("updated_at", detail.snapshot() == null ? null : detail.snapshot().getUpdatedAt());
        value.put("short_term_trend", parseJson(detail.shortTermTrendJson()));
        value.put("current_situation", parseJson(detail.currentSituationJson()));
        value.put("summary", parseJson(detail.summaryJson()));
        return value;
    }

    private Map<String, Object> target(ApplicationAnalysisPageBO.Target target, List<String> tags) {
        if (target == null) return null;
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", target.getTargetKey());
        value.put("target_type", target.getTargetType());
        value.put("symbol", target.getSymbol());
        value.put("market", target.getMarket());
        value.put("name", target.getName());
        value.put("adjust", target.getAdjust());
        value.put("enabled", target.getEnabled());
        value.put("interval_minutes", target.getIntervalMinutes());
        value.put("tags", tags == null ? List.of() : tags);
        value.put("last_updated_at", target.getUpdatedAt());
        return value;
    }

    private Object parseJson(String value) {
        if (value == null || value.isBlank()) return Map.of();
        try {
            return objectMapper.readTree(value);
        } catch (Exception ignored) {
            return value;
        }
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
