package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.AiUsageBreakdownDTO;
import cn.cccs7.cynexus.ai.api.dto.AiUsageObservationDTO;
import cn.cccs7.cynexus.ai.api.dto.AiUsagePageDTO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageObservationBO;
import cn.cccs7.cynexus.ai.domain.entity.usage.AiUsageBreakdownBO;
import cn.cccs7.cynexus.ai.domain.entity.usage.AiUsageReportBO;
import cn.cccs7.cynexus.ai.domain.service.usage.AiUsageQueryDomainService;
import org.springframework.stereotype.Service;

@Service
public class AiUsageAppService {

    private final AiUsageQueryDomainService aiUsageQueryDomainService;

    public AiUsageAppService(AiUsageQueryDomainService aiUsageQueryDomainService) {
        this.aiUsageQueryDomainService = aiUsageQueryDomainService;
    }

    public AiUsagePageDTO query(String fromText, String toText, Integer requestedLimit) {
        AiUsageReportBO report = aiUsageQueryDomainService.query(fromText, toText, requestedLimit);
        return AiUsagePageDTO.builder()
                .source(report.getSource())
                .available(report.isAvailable())
                .message(report.getMessage())
                .from(report.getFrom())
                .to(report.getTo())
                .requests(report.getRequests())
                .succeeded(report.getSucceeded())
                .failed(report.getFailed())
                .inputTokens(report.getInputTokens())
                .outputTokens(report.getOutputTokens())
                .totalTokens(report.getTotalTokens())
                .totalCost(report.getTotalCost())
                .breakdown(report.getBreakdown().stream().map(this::toDTO).toList())
                .observations(report.getObservations().stream().map(this::toDTO).toList())
                .build();
    }

    private AiUsageObservationDTO toDTO(AiUsageObservationBO value) {
        return AiUsageObservationDTO.builder()
                .id(value.getId())
                .name(value.getName())
                .providerCode(value.getProviderCode())
                .modelCode(value.getModelCode())
                .status(value.getStatus())
                .inputTokens(value.getInputTokens())
                .outputTokens(value.getOutputTokens())
                .totalTokens(value.getTotalTokens())
                .totalCost(value.getTotalCost())
                .startedAt(value.getStartedAt())
                .latencyMs(value.getLatencyMs())
                .statusMessage(value.getStatusMessage())
                .build();
    }

    private AiUsageBreakdownDTO toDTO(AiUsageBreakdownBO value) {
        return AiUsageBreakdownDTO.builder()
                .key(value.getKey())
                .providerCode(value.getProviderCode())
                .modelCode(value.getModelCode())
                .capability(value.getCapability())
                .requests(value.getRequests())
                .succeeded(value.getSucceeded())
                .failed(value.getFailed())
                .inputTokens(value.getInputTokens())
                .outputTokens(value.getOutputTokens())
                .totalTokens(value.getTotalTokens())
                .totalCost(value.getTotalCost())
                .build();
    }
}
