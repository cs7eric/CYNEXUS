package cn.cccs7.cynexus.ai.infra.ai.adapter;

public interface AiModelAdapter {

    String providerCode();

    String complete(String prompt);
}
