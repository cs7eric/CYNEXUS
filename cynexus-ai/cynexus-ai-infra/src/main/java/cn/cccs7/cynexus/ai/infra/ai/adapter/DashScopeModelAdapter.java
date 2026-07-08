package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.infra.secret.SecretResolver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DashScopeModelAdapter extends OpenAiCompatibleChatAdapter {

    public DashScopeModelAdapter(SecretResolver secretResolver,
                                 Environment environment) {
        super(secretResolver, environment);
    }

    @Override
    public String providerCode() {
        return "dashscope";
    }

    @Override
    protected String baseUrlProperty() {
        return "cynexus.ai.platform.providers.dashscope.base-url";
    }

    @Override
    protected String apiKeyEnvProperty() {
        return "cynexus.ai.platform.providers.dashscope.api-key-env";
    }

    @Override
    protected String defaultBaseUrl() {
        return "https://dashscope.aliyuncs.com/compatible-mode";
    }

    @Override
    protected String defaultApiKeyEnv() {
        return "CYNEXUS_AI_DASHSCOPE_API_KEY";
    }

    @Override
    protected String defaultModelName() {
        return "qwen-plus";
    }
}
