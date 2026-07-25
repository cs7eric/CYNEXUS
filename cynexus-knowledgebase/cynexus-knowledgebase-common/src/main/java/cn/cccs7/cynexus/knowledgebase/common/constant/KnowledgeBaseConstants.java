package cn.cccs7.cynexus.knowledgebase.common.constant;

public final class KnowledgeBaseConstants {

    public static final int MAX_SEARCH_KEYWORD_LENGTH = 240;
    public static final int MAX_SEARCH_TOKENS = 32;
    public static final int MAX_SEARCH_CANDIDATES = 100;
    public static final int MAX_CHUNK_LENGTH = 1_200;
    public static final int MAX_AGENT_SEARCH_RESULTS = 10;
    public static final String CONTENT_FORMAT_MARKDOWN = "markdown";
    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_ARCHIVED = "archived";
    public static final String INDEX_TARGET_PGVECTOR = "pgvector";
    public static final String INDEX_TARGET_ELASTICSEARCH = "elasticsearch";
    public static final String INDEX_STATUS_PENDING = "pending";

    private KnowledgeBaseConstants() {
    }
}
