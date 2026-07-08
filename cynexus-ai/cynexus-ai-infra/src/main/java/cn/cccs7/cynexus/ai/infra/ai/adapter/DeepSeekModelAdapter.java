package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekModelAdapter extends OpenAiCompatibleChatAdapter {

    public DeepSeekModelAdapter(SecretResolver secretResolver,
                                Environment environment) {
        super(secretResolver, environment);
    }

    @Override
    public String providerCode() {
        return "deepseek";
    }

    @Override
    protected String baseUrlProperty() {
        return "cynexus.ai.platform.providers.deepseek.base-url";
    }

    @Override
    protected String apiKeyEnvProperty() {
        return "cynexus.ai.platform.providers.deepseek.api-key-env";
    }

    @Override
    protected String defaultBaseUrl() {
        return "https://api.deepseek.com";
    }

    @Override
    protected String defaultApiKeyEnv() {
        return "CYNEXUS_AI_DEEPSEEK_API_KEY";
    }

    @Override
    protected String defaultModelName() {
        return "deepseek-chat";
    }
}
