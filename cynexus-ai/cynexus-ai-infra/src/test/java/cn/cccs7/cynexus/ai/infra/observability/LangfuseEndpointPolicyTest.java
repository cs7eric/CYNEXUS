package cn.cccs7.cynexus.ai.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class LangfuseEndpointPolicyTest {
    @Test
    void disablesUsageReadsWhenConfiguredEndpointIsOutsideTheAllowList() {
        LangfuseUsageRepositoryImpl repository = new LangfuseUsageRepositoryImpl(
                true,
                "https://untrusted.example.com",
                "public-key",
                "secret-key",
                "https://cynexus-langfuse.example.com");

        assertThat(repository.list(OffsetDateTime.now().minusHours(1), OffsetDateTime.now(), 10)).isEmpty();
    }

    @Test
    void keepsReporterFailOpenWhenConfiguredEndpointIsOutsideTheAllowList() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> new LangfuseGenerationReporter(
                true,
                "https://untrusted.example.com",
                "public-key",
                "secret-key",
                "https://cynexus-langfuse.example.com"));
    }
}
