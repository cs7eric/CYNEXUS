package cn.cccs7.cynexus.ai.infra.agent.finance;

public final class FinanceAgentToolSchemas {
    private FinanceAgentToolSchemas() {
    }

    public static final String DATE_RANGE = "{\"type\":\"object\",\"properties\":{\"start\":{\"type\":\"string\",\"format\":\"date\"},\"end\":{\"type\":\"string\",\"format\":\"date\"}},\"additionalProperties\":false}";
    public static final String TRANSACTIONS = "{\"type\":\"object\",\"properties\":{\"start\":{\"type\":\"string\",\"format\":\"date\"},\"end\":{\"type\":\"string\",\"format\":\"date\"},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":100}},\"additionalProperties\":false}";
    public static final String OVERVIEW_OUTPUT = "{\"type\":\"object\",\"required\":[\"startDate\",\"endDate\",\"income\",\"expense\",\"net\",\"netAssets\"],\"properties\":{\"startDate\":{\"type\":[\"string\",\"null\"],\"format\":\"date\"},\"endDate\":{\"type\":[\"string\",\"null\"],\"format\":\"date\"},\"income\":{\"type\":[\"number\",\"null\"]},\"expense\":{\"type\":[\"number\",\"null\"]},\"net\":{\"type\":[\"number\",\"null\"]},\"assetTotal\":{\"type\":[\"number\",\"null\"]},\"liabilityTotal\":{\"type\":[\"number\",\"null\"]},\"netAssets\":{\"type\":[\"number\",\"null\"]},\"accounts\":{\"type\":[\"array\",\"null\"]},\"categories\":{\"type\":[\"array\",\"null\"]},\"daily\":{\"type\":[\"array\",\"null\"]},\"recentTransactions\":{\"type\":[\"array\",\"null\"]}}}";
    public static final String TRANSACTIONS_OUTPUT = "{\"type\":\"array\",\"items\":{\"type\":\"object\",\"required\":[\"id\",\"amount\",\"occurredAt\"],\"properties\":{\"id\":{\"type\":\"integer\"},\"transactionType\":{\"type\":[\"string\",\"null\"]},\"accountId\":{\"type\":[\"integer\",\"null\"]},\"accountName\":{\"type\":[\"string\",\"null\"]},\"categoryName\":{\"type\":[\"string\",\"null\"]},\"amount\":{\"type\":[\"number\",\"null\"]},\"currency\":{\"type\":[\"string\",\"null\"]},\"occurredAt\":{\"type\":[\"string\",\"null\"],\"format\":\"date-time\"},\"merchant\":{\"type\":[\"string\",\"null\"]},\"note\":{\"type\":[\"string\",\"null\"]},\"source\":{\"type\":[\"string\",\"null\"]}}}}";
}
