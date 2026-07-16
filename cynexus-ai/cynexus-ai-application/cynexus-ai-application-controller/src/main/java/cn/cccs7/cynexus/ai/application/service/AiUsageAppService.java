package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.AiUsageBreakdownDTO;
import cn.cccs7.cynexus.ai.api.dto.AiUsageObservationDTO;
import cn.cccs7.cynexus.ai.api.dto.AiUsagePageDTO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageObservationBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageObservabilityRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiUsageAppService {
    private final AiUsageObservabilityRepository repository;
    private final AiProviderRepository providerRepository;

    public AiUsageAppService(AiUsageObservabilityRepository repository, AiProviderRepository providerRepository) {
        this.repository = repository;
        this.providerRepository = providerRepository;
    }

    public AiUsagePageDTO query(String fromText, String toText, Integer requestedLimit) {
        OffsetDateTime to = parse(toText, OffsetDateTime.now(ZoneOffset.UTC));
        OffsetDateTime from = parse(fromText, to.minusDays(7));
        int limit = requestedLimit == null ? 200 : Math.max(1, Math.min(requestedLimit, 1000));
        List<AiUsageObservationBO> observations = repository.list(from, to, limit);
        Map<String, AiProviderBO> pricing = providerRepository.listAll().stream()
                .flatMap(item -> java.util.stream.Stream.of(
                        Map.entry(safe(item.getCode()).toLowerCase(), item),
                        Map.entry(safe(item.getProviderType()).toLowerCase(), item)))
                .filter(item -> !item.getKey().isBlank())
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
        Map<String, Aggregate> aggregates = new LinkedHashMap<>();
        for (AiUsageObservationBO observation : observations) {
            String capability = observation.getName() == null ? "unknown" : observation.getName();
            String key = safe(observation.getProviderCode()) + ":" + safe(observation.getModelCode()) + ":" + capability;
            aggregates.computeIfAbsent(key, ignored -> new Aggregate(observation.getProviderCode(), observation.getModelCode(), capability))
                    .add(observation, costFor(observation, pricing));
        }
        long succeeded = observations.stream().filter(item -> "SUCCEEDED".equalsIgnoreCase(item.getStatus())).count();
        long failed = observations.size() - succeeded;
        return AiUsagePageDTO.builder()
                .source("langfuse")
                .available(!observations.isEmpty())
                .message(observations.isEmpty() ? "Langfuse is unavailable or has no observations for this range." : null)
                .from(from).to(to)
                .requests(observations.size()).succeeded(succeeded).failed(failed)
                .inputTokens(observations.stream().mapToLong(item -> value(item.getInputTokens())).sum())
                .outputTokens(observations.stream().mapToLong(item -> value(item.getOutputTokens())).sum())
                .totalTokens(observations.stream().mapToLong(item -> value(item.getTotalTokens())).sum())
                .totalCost(observations.stream().map(item -> costFor(item, pricing)).reduce(BigDecimal.ZERO, BigDecimal::add))
                .breakdown(aggregates.values().stream().map(Aggregate::toDTO).toList())
                .observations(observations.stream().map(item -> toDTO(item, pricing)).toList())
                .build();
    }

    private AiUsageObservationDTO toDTO(AiUsageObservationBO value, Map<String, AiProviderBO> pricing) {
        return AiUsageObservationDTO.builder().id(value.getId()).name(value.getName())
                .providerCode(value.getProviderCode()).modelCode(value.getModelCode()).status(value.getStatus())
                .inputTokens(value.getInputTokens()).outputTokens(value.getOutputTokens()).totalTokens(value.getTotalTokens())
                .totalCost(costFor(value, pricing)).startedAt(value.getStartedAt()).latencyMs(value.getLatencyMs())
                .statusMessage(value.getStatusMessage()).build();
    }

    private static OffsetDateTime parse(String value, OffsetDateTime fallback) {
        try { return value == null || value.isBlank() ? fallback : OffsetDateTime.parse(value); }
        catch (Exception ignored) { return fallback; }
    }

    private static long value(Integer value) { return value == null ? 0 : Math.max(0, value); }
    private static String safe(String value) { return value == null ? "" : value; }

    private static BigDecimal costFor(AiUsageObservationBO item, Map<String, AiProviderBO> pricing) {
        if (item.getTotalCost() != null) return item.getTotalCost();
        AiProviderBO provider = pricing.getOrDefault(safe(item.getProviderCode()).toLowerCase(), null);
        if (provider == null) return BigDecimal.ZERO;
        BigDecimal input = provider.getInputPricePerMillion() == null ? BigDecimal.ZERO : provider.getInputPricePerMillion();
        BigDecimal output = provider.getOutputPricePerMillion() == null ? BigDecimal.ZERO : provider.getOutputPricePerMillion();
        return BigDecimal.valueOf(value(item.getInputTokens())).multiply(input)
                .add(BigDecimal.valueOf(value(item.getOutputTokens())).multiply(output))
                .divide(BigDecimal.valueOf(1_000_000L), 12, java.math.RoundingMode.HALF_UP);
    }

    private static final class Aggregate {
        private final String providerCode;
        private final String modelCode;
        private final String capability;
        private long requests;
        private long succeeded;
        private long failed;
        private long input;
        private long output;
        private long total;
        private BigDecimal cost = BigDecimal.ZERO;

        private Aggregate(String providerCode, String modelCode, String capability) {
            this.providerCode = providerCode;
            this.modelCode = modelCode;
            this.capability = capability;
        }

        private void add(AiUsageObservationBO item, BigDecimal calculatedCost) {
            requests++;
            if ("SUCCEEDED".equalsIgnoreCase(item.getStatus())) succeeded++; else failed++;
            input += value(item.getInputTokens()); output += value(item.getOutputTokens()); total += value(item.getTotalTokens());
            cost = cost.add(calculatedCost == null ? BigDecimal.ZERO : calculatedCost);
        }

        private AiUsageBreakdownDTO toDTO() {
            return AiUsageBreakdownDTO.builder().key(providerCode + ":" + modelCode + ":" + capability)
                    .providerCode(providerCode).modelCode(modelCode).capability(capability)
                    .requests(requests).succeeded(succeeded).failed(failed).inputTokens(input).outputTokens(output)
                    .totalTokens(total).totalCost(cost).build();
        }
    }
}
