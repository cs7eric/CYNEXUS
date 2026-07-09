package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class MinimaxModelAdapter extends OpenAiCompatibleChatAdapter {

    public MinimaxModelAdapter(SecretResolver secretResolver,
                               Environment environment) {
        super(secretResolver, environment);
    }

    @Override
    public String providerCode() {
        return "minimax";
    }

    @Override
    protected String baseUrlProperty() {
        return "cynexus.ai.platform.providers.minimax.base-url";
    }

    @Override
    protected String apiKeyEnvProperty() {
        return "cynexus.ai.platform.providers.minimax.api-key-env";
    }

    @Override
    protected String defaultBaseUrl() {
        return "https://api.minimaxi.com";
    }

    @Override
    protected String defaultApiKeyEnv() {
        return "MINIMAX_API_KEY";
    }

    @Override
    protected String defaultModelName() {
        return "MiniMax-M2.5";
    }
}
