package cn.cccs7.cynexus.ai.domain.service.usage;

import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageObservationBO;
import cn.cccs7.cynexus.ai.domain.entity.usage.AiUsageBreakdownBO;
import cn.cccs7.cynexus.ai.domain.entity.usage.AiUsageReportBO;
import cn.cccs7.cynexus.ai.domain.policy.provider.AiProviderModelPricingPolicy;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageObservabilityRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AiUsageQueryDomainServiceImpl implements AiUsageQueryDomainService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;

    private final AiUsageObservabilityRepository usageObservabilityRepository;
    private final AiProviderRepository providerRepository;
    private final Clock clock;

    public AiUsageQueryDomainServiceImpl(AiUsageObservabilityRepository usageObservabilityRepository,
                                         AiProviderRepository providerRepository) {
        this(usageObservabilityRepository, providerRepository, Clock.systemUTC());
    }

    public AiUsageQueryDomainServiceImpl(AiUsageObservabilityRepository usageObservabilityRepository,
                                         AiProviderRepository providerRepository,
                                         Clock clock) {
        this.usageObservabilityRepository = Objects.requireNonNull(
                usageObservabilityRepository, "usageObservabilityRepository");
        this.providerRepository = Objects.requireNonNull(providerRepository, "providerRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public AiUsageReportBO query(String fromText, String toText, Integer requestedLimit) {
        OffsetDateTime to = parse(toText, OffsetDateTime.now(clock));
        OffsetDateTime from = parse(fromText, to.minusDays(7));
        int limit = requestedLimit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
        List<AiUsageObservationBO> observations = usageObservabilityRepository.list(from, to, limit);
        if (observations == null) {
            observations = List.of();
        }

        Map<String, AiProviderBO> pricing = providerRepository.listAll().stream()
                .flatMap(item -> java.util.stream.Stream.of(
                        Map.entry(safe(item.getCode()).toLowerCase(), item),
                        Map.entry(safe(item.getProviderType()).toLowerCase(), item)))
                .filter(item -> !item.getKey().isBlank())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
        Map<String, Aggregate> aggregates = new LinkedHashMap<>();
        for (AiUsageObservationBO observation : observations) {
            String capability = observation.getName() == null ? "unknown" : observation.getName();
            String key = safe(observation.getProviderCode()) + ":"
                    + safe(observation.getModelCode()) + ":" + capability;
            aggregates.computeIfAbsent(key, ignored -> new Aggregate(
                            observation.getProviderCode(), observation.getModelCode(), capability))
                    .add(observation, costFor(observation, pricing));
        }
        List<AiUsageObservationBO> pricedObservations = observations.stream()
                .map(observation -> withCost(observation, costFor(observation, pricing)))
                .toList();

        long succeeded = observations.stream()
                .filter(item -> "SUCCEEDED".equalsIgnoreCase(item.getStatus()))
                .count();
        long failed = observations.size() - succeeded;
        return AiUsageReportBO.builder()
                .source("langfuse")
                .available(!observations.isEmpty())
                .message(observations.isEmpty()
                        ? "Langfuse is unavailable or has no observations for this range."
                        : null)
                .from(from)
                .to(to)
                .requests(observations.size())
                .succeeded(succeeded)
                .failed(failed)
                .inputTokens(observations.stream().mapToLong(item -> value(item.getInputTokens())).sum())
                .outputTokens(observations.stream().mapToLong(item -> value(item.getOutputTokens())).sum())
                .totalTokens(observations.stream().mapToLong(item -> value(item.getTotalTokens())).sum())
                .totalCost(observations.stream()
                        .map(item -> costFor(item, pricing))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .breakdown(aggregates.values().stream().map(Aggregate::toBO).toList())
                .observations(pricedObservations)
                .build();
    }

    private static OffsetDateTime parse(String value, OffsetDateTime fallback) {
        try {
            return value == null || value.isBlank() ? fallback : OffsetDateTime.parse(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long value(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static BigDecimal costFor(AiUsageObservationBO item, Map<String, AiProviderBO> pricing) {
        AiProviderBO provider = pricing.get(safe(item.getProviderCode()).toLowerCase());
        AiProviderModelPricingPolicy.ModelPricing modelPricing = provider == null
                ? null : AiProviderModelPricingPolicy.find(provider, item.getModelCode()).orElse(null);
        if (modelPricing == null && provider == null) {
            return item.getTotalCost() == null ? BigDecimal.ZERO : item.getTotalCost();
        }
        if (modelPricing == null && item.getTotalCost() != null) {
            return item.getTotalCost();
        }
        BigDecimal input = modelPricing == null ? provider.getInputPricePerMillion() : modelPricing.inputPricePerMillion();
        BigDecimal output = modelPricing == null ? provider.getOutputPricePerMillion() : modelPricing.outputPricePerMillion();
        input = input == null ? BigDecimal.ZERO : input;
        output = output == null ? BigDecimal.ZERO : output;
        return BigDecimal.valueOf(value(item.getInputTokens()))
                .multiply(input)
                .add(BigDecimal.valueOf(value(item.getOutputTokens())).multiply(output))
                .divide(BigDecimal.valueOf(1_000_000L), 12, RoundingMode.HALF_UP);
    }

    private static AiUsageObservationBO withCost(AiUsageObservationBO item, BigDecimal cost) {
        return AiUsageObservationBO.builder()
                .id(item.getId())
                .name(item.getName())
                .providerCode(item.getProviderCode())
                .modelCode(item.getModelCode())
                .status(item.getStatus())
                .inputTokens(item.getInputTokens())
                .outputTokens(item.getOutputTokens())
                .totalTokens(item.getTotalTokens())
                .totalCost(cost)
                .startedAt(item.getStartedAt())
                .latencyMs(item.getLatencyMs())
                .statusMessage(item.getStatusMessage())
                .build();
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
            if ("SUCCEEDED".equalsIgnoreCase(item.getStatus())) {
                succeeded++;
            } else {
                failed++;
            }
            input += value(item.getInputTokens());
            output += value(item.getOutputTokens());
            total += value(item.getTotalTokens());
            cost = cost.add(calculatedCost == null ? BigDecimal.ZERO : calculatedCost);
        }

        private AiUsageBreakdownBO toBO() {
            return AiUsageBreakdownBO.builder()
                    .key(providerCode + ":" + modelCode + ":" + capability)
                    .providerCode(providerCode)
                    .modelCode(modelCode)
                    .capability(capability)
                    .requests(requests)
                    .succeeded(succeeded)
                    .failed(failed)
                    .inputTokens(input)
                    .outputTokens(output)
                    .totalTokens(total)
                    .totalCost(cost)
                    .build();
        }
    }
}
