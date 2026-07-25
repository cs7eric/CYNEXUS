package cn.cccs7.cynexus.ai.infra.agent.knowledge;

final class KnowledgeAgentToolSchemas {

    static final String SEARCH = "{\"type\":\"object\",\"required\":[\"query\"],\"properties\":{\"query\":{\"type\":\"string\",\"minLength\":2,\"maxLength\":240,\"description\":\"Keyword query for published knowledge documents.\"},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":10,\"description\":\"Maximum number of matching documents to return.\"}},\"additionalProperties\":false}";
    static final String SEARCH_OUTPUT = "{\"type\":\"array\",\"items\":{\"type\":\"object\",\"required\":[\"id\",\"title\",\"status\"],\"properties\":{\"id\":{\"type\":\"integer\",\"description\":\"Document identifier.\"},\"title\":{\"type\":\"string\",\"description\":\"Document title.\"},\"description\":{\"type\":\"string\",\"description\":\"Document summary when available.\"},\"status\":{\"type\":\"string\",\"description\":\"Document publication status.\"},\"updatedAt\":{\"type\":[\"string\",\"null\"],\"description\":\"Last update timestamp when available.\"}}}}";
    static final String DOCUMENT = "{\"type\":\"object\",\"required\":[\"documentId\"],\"properties\":{\"documentId\":{\"type\":\"integer\",\"minimum\":1,\"description\":\"Positive document identifier returned by knowledge.search.\"}},\"additionalProperties\":false}";
    static final String DOCUMENT_OUTPUT = "{\"type\":\"object\",\"required\":[\"id\",\"title\",\"status\",\"content\"],\"properties\":{\"id\":{\"type\":\"integer\",\"description\":\"Document identifier.\"},\"title\":{\"type\":\"string\",\"description\":\"Document title.\"},\"description\":{\"type\":[\"string\",\"null\"],\"description\":\"Document summary when available.\"},\"content\":{\"type\":\"string\",\"description\":\"Full document content.\"},\"contentFormat\":{\"type\":[\"string\",\"null\"],\"description\":\"Content format such as markdown.\"},\"status\":{\"type\":\"string\",\"description\":\"Document publication status.\"},\"updatedAt\":{\"type\":[\"string\",\"null\"],\"format\":\"date-time\",\"description\":\"Last update timestamp when available.\"}}}";

    private KnowledgeAgentToolSchemas() {
    }
}
