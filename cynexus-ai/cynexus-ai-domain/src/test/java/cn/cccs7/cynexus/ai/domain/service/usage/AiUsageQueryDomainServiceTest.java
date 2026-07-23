package cn.cccs7.cynexus.ai.domain.service.usage;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageObservationBO;
import cn.cccs7.cynexus.ai.domain.entity.usage.AiUsageReportBO;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageObservabilityRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiUsageQueryDomainServiceTest {

    @Test
    void normalizesQueryBoundsCalculatesCostAndAggregatesObservations() {
        AiUsageObservationBO priced = AiUsageObservationBO.builder()
                .id("one").name("chat").providerCode("openai").modelCode("gpt")
                .status("SUCCEEDED").inputTokens(1_000).outputTokens(2_000).totalTokens(3_000).build();
        AiUsageObservationBO explicitCost = AiUsageObservationBO.builder()
                .id("two").name("chat").providerCode("openai").modelCode("gpt")
                .status("FAILED").inputTokens(-1).outputTokens(null).totalTokens(null)
                .totalCost(new BigDecimal("0.500000")).build();
        AiUsageQueryDomainService service = new AiUsageQueryDomainServiceImpl(
                (from, to, limit) -> List.of(priced, explicitCost),
                providers(),
                Clock.fixed(Instant.parse("2026-07-23T00:00:00Z"), ZoneOffset.UTC));

        AiUsageReportBO report = service.query("invalid", null, 5000);

        assertThat(report.getFrom()).isEqualTo(Instant.parse("2026-07-16T00:00:00Z").atOffset(ZoneOffset.UTC));
        assertThat(report.getTo()).isEqualTo(Instant.parse("2026-07-23T00:00:00Z").atOffset(ZoneOffset.UTC));
        assertThat(report.getRequests()).isEqualTo(2);
        assertThat(report.getSucceeded()).isEqualTo(1);
        assertThat(report.getFailed()).isEqualTo(1);
        assertThat(report.getTotalCost()).isEqualByComparingTo("0.510000000000");
        assertThat(report.getBreakdown()).singleElement().satisfies(item -> {
            assertThat(item.getKey()).isEqualTo("openai:gpt:chat");
            assertThat(item.getInputTokens()).isEqualTo(1000);
            assertThat(item.getTotalCost()).isEqualByComparingTo("0.510000000000");
        });
        assertThat(report.getObservations().get(0).getTotalCost()).isEqualByComparingTo("0.010000000000");
    }

    @Test
    void usesDefaultLimitAndEmptyAvailabilityWhenRepositoryReturnsNoObservations() {
        int[] capturedLimit = new int[1];
        AiUsageQueryDomainService service = new AiUsageQueryDomainServiceImpl(
                (from, to, limit) -> {
                    capturedLimit[0] = limit;
                    return List.of();
                },
                providers());

        AiUsageReportBO report = service.query(null, null, null);

        assertThat(capturedLimit[0]).isEqualTo(200);
        assertThat(report.isAvailable()).isFalse();
        assertThat(report.getMessage()).contains("Langfuse");
    }

    private AiProviderRepository providers() {
        AiProviderBO provider = AiProviderBO.builder()
                .code("openai")
                .providerType("openai")
                .inputPricePerMillion(new BigDecimal("2"))
                .outputPricePerMillion(new BigDecimal("4"))
                .build();
        return new AiProviderRepository() {
            @Override public List<AiProviderBO> listAll() { return List.of(provider); }
            @Override public AiProviderBO findById(Long id) { return null; }
            @Override public Long create(AiProviderBO value) { return null; }
            @Override public boolean update(AiProviderBO value) { return false; }
            @Override public boolean delete(Long id) { return false; }
        };
    }
}
