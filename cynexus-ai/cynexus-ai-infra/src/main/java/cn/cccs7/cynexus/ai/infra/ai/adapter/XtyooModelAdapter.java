package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * OpenAI-compatible adapter for the xtyoo provider.
 *
 * The endpoint and credential remain database/environment driven, while the
 * provider type gives the dynamic adapter registry a stable runtime key.
 */
@Component
public class XtyooModelAdapter extends OpenAiCompatibleChatAdapter {

    public XtyooModelAdapter(SecretResolver secretResolver, Environment environment) {
        super(secretResolver, environment);
    }

    @Override
    public String providerCode() {
        return "xtyoo";
    }

    @Override
    protected String baseUrlProperty() {
        return "cynexus.ai.platform.providers.xtyoo.base-url";
    }

    @Override
    protected String apiKeyEnvProperty() {
        return "cynexus.ai.platform.providers.xtyoo.api-key-env";
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
        return "gpt-6.1-sol";
    }
}
