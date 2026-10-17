package cn.cccs7.cynexus.ai.infra.ai.spring;

import com.openai.client.OpenAIClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.ai.openai.setup.OpenAiSetup;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;

public final class SpringAiOpenAiClientSupport {
    private SpringAiOpenAiClientSupport() { }
    public static OpenAIClient client(String baseUrl, String apiKey, Duration timeout) {
        return OpenAiSetup.setupSyncClient(normalizeBaseUrl(baseUrl), apiKey, null, null, null, null, false, false, null,
                timeout, 0, null, Map.of(), ObservationRegistry.NOOP, new SimpleMeterRegistry(), List.of());
    }

    /**
     * Spring AI's OpenAI client appends resource paths (for example
     * {@code /embeddings}) directly to the configured base URL. OpenAI-compatible
     * providers expose those resources below {@code /v1}; callers in Cynexus
     * persist the provider origin or compatible-mode root, so normalize it here
     * instead of requiring every provider configuration to know this client
     * detail.
     */
    static String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        if (value.isBlank() || value.matches(".*/v\\d+$")) return value;
        return value + "/v1";
    }
}
