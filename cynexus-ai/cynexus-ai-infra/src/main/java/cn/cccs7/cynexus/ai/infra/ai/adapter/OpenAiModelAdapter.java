package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OpenAiModelAdapter extends OpenAiCompatibleChatAdapter {

    public OpenAiModelAdapter(SecretResolver secretResolver,
                              Environment environment) {
        super(secretResolver, environment);
    }

    @Override
    public String providerCode() {
        return "openai";
    }

    @Override
    protected String baseUrlProperty() {
        return "cynexus.ai.platform.providers.openai.base-url";
    }

    @Override
    protected String apiKeyEnvProperty() {
        return "cynexus.ai.platform.providers.openai.api-key-env";
    }

    @Override
    protected String defaultBaseUrl() {
        return "https://api.openai.com";
    }

    @Override
    protected String defaultApiKeyEnv() {
        return "CYNEXUS_AI_OPENAI_API_KEY";
    }

    @Override
    protected String defaultModelName() {
        return "gpt-4o-mini";
    }
}
