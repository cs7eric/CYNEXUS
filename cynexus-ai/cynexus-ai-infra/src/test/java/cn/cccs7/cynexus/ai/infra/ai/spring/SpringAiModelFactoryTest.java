package cn.cccs7.cynexus.ai.infra.ai.spring;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import org.junit.jupiter.api.Test;

class SpringAiModelFactoryTest {

    @Test
    void normalizesOpenAiCompatibleBaseUrlForSpringAiResources() {
        org.junit.jupiter.api.Assertions.assertEquals(
                "https://provider.example/v1",
                SpringAiOpenAiClientSupport.normalizeBaseUrl("https://provider.example/"));
        org.junit.jupiter.api.Assertions.assertEquals(
                "https://provider.example/compatible-mode/v1",
                SpringAiOpenAiClientSupport.normalizeBaseUrl("https://provider.example/compatible-mode"));
        org.junit.jupiter.api.Assertions.assertEquals(
                "https://provider.example/v1",
                SpringAiOpenAiClientSupport.normalizeBaseUrl("https://provider.example/v1/"));
    }

    @Test
    void cachesOnlyEquivalentRoutedClientConfiguration() {
        SpringAiModelFactory factory = new SpringAiModelFactory(ref -> "env-key", "https://provider.example");
        AiModelBO first = model("key-a", "{\"temperature\":0.2}");
        AiModelBO same = model("key-a", "{\"temperature\":0.2}");
        AiModelBO changedOptions = model("key-a", "{\"temperature\":0.3}");

        assertSame(factory.resolve(first), factory.resolve(same));
        assertNotSame(factory.resolve(first), factory.resolve(changedOptions));
    }

    @Test
    void resolvesEnvironmentSecretAndRejectsMissingCredentials() {
        SecretResolver secrets = ref -> "env://CYNEXUS_AI_KEY".equals(ref) ? "resolved-key" : "";
        SpringAiModelFactory factory = new SpringAiModelFactory(secrets, "https://provider.example");
        AiModelBO viaEnvironment = model(null, "{}");
        viaEnvironment.setApiKeyEnv("CYNEXUS_AI_KEY");
        assertSame(factory.resolve(viaEnvironment), factory.resolve(viaEnvironment));

        AiModelBO missing = model(null, "{}");
        assertThrows(IllegalStateException.class, () -> factory.resolve(missing));
    }

    @Test
    void doesNotForceJsonForModelsThatMerelySupportStructuredOutput() {
        SpringAiModelFactory factory = new SpringAiModelFactory(ref -> "env-key", "https://provider.example");
        var chatModel = factory.resolve(model("key-a", "{}"));

        assertNull(((org.springframework.ai.openai.OpenAiChatOptions) chatModel.getDefaultOptions()).getResponseFormat());
    }

    @Test
    void enablesJsonOnlyWhenRuntimeOptionRequestsIt() {
        SpringAiModelFactory factory = new SpringAiModelFactory(ref -> "env-key", "https://provider.example");
        var chatModel = factory.resolve(model("key-a", "{\"response_format\":\"json_object\"}"));

        assertSame(org.springframework.ai.openai.OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT,
                ((org.springframework.ai.openai.OpenAiChatOptions) chatModel.getDefaultOptions())
                        .getResponseFormat().getType());
    }

    private AiModelBO model(String apiKey, String extra) {
        return AiModelBO.builder().providerCode("test").modelCode("test-model").modelName("test-model")
                .baseUrl("https://provider.example").apiKey(apiKey).extra(extra)
                .capability(ModelCapability.builder().structuredOutputSupported(true).build()).enabled(true).build();
    }
}
