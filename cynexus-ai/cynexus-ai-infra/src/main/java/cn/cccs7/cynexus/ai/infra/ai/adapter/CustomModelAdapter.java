package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CustomModelAdapter extends OpenAiCompatibleChatAdapter {

    public CustomModelAdapter(SecretResolver secretResolver, Environment environment) {
        super(secretResolver, environment);
    }

    @Override
    public String providerCode() {
        return "custom";
    }

    @Override
    protected String baseUrlProperty() {
        return "cynexus.ai.platform.providers.custom.base-url";
    }

    @Override
    protected String apiKeyEnvProperty() {
        return "cynexus.ai.platform.providers.custom.api-key-env";
    }

    @Override
    protected String defaultBaseUrl() {
        return "https://am.xtyooo.top";
    }

    @Override
    protected String defaultApiKeyEnv() {
        return "CYNEXUS_AI_CUSTOM_API_KEY";
    }

    @Override
    protected String defaultModelName() {
        return "gpt-5.5";
    }
}
