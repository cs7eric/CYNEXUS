package cn.cccs7.cynexus.ai.infra.agent.finance;

public final class FinanceAgentToolSchemas {
    private FinanceAgentToolSchemas() {
    }

    public static final String DATE_RANGE = "{\"type\":\"object\",\"properties\":{\"start\":{\"type\":\"string\",\"format\":\"date\"},\"end\":{\"type\":\"string\",\"format\":\"date\"}}}";
    public static final String TRANSACTIONS = "{\"type\":\"object\",\"properties\":{\"start\":{\"type\":\"string\",\"format\":\"date\"},\"end\":{\"type\":\"string\",\"format\":\"date\"},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":100}}}";
}
