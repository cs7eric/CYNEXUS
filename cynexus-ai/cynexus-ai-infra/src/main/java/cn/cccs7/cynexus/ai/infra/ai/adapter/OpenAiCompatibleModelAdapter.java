package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Adapter for providers stored with the generic openai_compatible provider type.
 * The provider record supplies the concrete endpoint and credential at runtime.
 */
@Component
public class OpenAiCompatibleModelAdapter extends OpenAiCompatibleChatAdapter {

    public OpenAiCompatibleModelAdapter(SecretResolver secretResolver, Environment environment) {
        super(secretResolver, environment);
    }

    @Override
    public String providerCode() {
        return "openai_compatible";
    }

    @Override
    protected String baseUrlProperty() {
        return "cynexus.ai.platform.providers.openai-compatible.base-url";
    }

    @Override
    protected String apiKeyEnvProperty() {
        return "cynexus.ai.platform.providers.openai-compatible.api-key-env";
    }

    @Override
    protected String defaultBaseUrl() {
        return "https://api.openai.com";
    }

    @Override
    protected String defaultApiKeyEnv() {
        return "OPENAI_API_KEY";
    }

    @Override
    protected String defaultModelName() {
        return "auto";
    }
}
