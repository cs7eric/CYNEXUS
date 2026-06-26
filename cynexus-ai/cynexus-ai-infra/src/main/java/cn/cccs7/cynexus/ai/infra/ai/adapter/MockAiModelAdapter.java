package cn.cccs7.cynexus.ai.infra.ai.adapter;

import org.springframework.stereotype.Component;

@Component
public class MockAiModelAdapter implements AiModelAdapter {

    @Override
    public String providerCode() {
        return "mock";
    }

    @Override
    public String complete(String prompt) {
        return "{\"content\":\"mock completion\"}";
    }
}
