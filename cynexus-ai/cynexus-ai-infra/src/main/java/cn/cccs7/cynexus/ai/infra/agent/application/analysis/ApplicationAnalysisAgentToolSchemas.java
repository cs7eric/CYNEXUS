package cn.cccs7.cynexus.ai.infra.agent.application.analysis;

final class ApplicationAnalysisAgentToolSchemas {

    static final String RESULT = "{\"type\":\"object\",\"required\":[\"targetId\"],\"properties\":{\"targetId\":{\"type\":\"string\",\"minLength\":1,\"maxLength\":128,\"description\":\"Configured application-analysis target identifier.\"}},\"additionalProperties\":false}";
    static final String RECENT30 = "{\"type\":\"object\",\"required\":[\"targetId\"],\"properties\":{\"targetId\":{\"type\":\"string\",\"minLength\":1,\"maxLength\":128,\"description\":\"Configured application-analysis target identifier.\"},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":60,\"description\":\"Number of recent daily snapshots to return.\"}},\"additionalProperties\":false}";

    private ApplicationAnalysisAgentToolSchemas() {
    }
}
