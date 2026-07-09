package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AnthropicCompatibleModelAdapter extends OpenAiCompatibleChatAdapter {

    public AnthropicCompatibleModelAdapter(SecretResolver secretResolver,
                                           Environment environment) {
        super(secretResolver, environment);
    }

    @Override
    public String providerCode() {
        return "anthropic_compatible";
    }

    @Override
    protected String baseUrlProperty() {
        return "cynexus.ai.platform.providers.anthropic-compatible.base-url";
    }

    @Override
    protected String apiKeyEnvProperty() {
        return "cynexus.ai.platform.providers.anthropic-compatible.api-key-env";
    }

    @Override
    protected String defaultBaseUrl() {
        return "https://api.anthropic.com";
    }

    @Override
    protected String defaultApiKeyEnv() {
        return "ANTHROPIC_API_KEY";
    }

    @Override
    protected String defaultModelName() {
        return "claude-sonnet-4-20250514";
    }
}
