package cn.cccs7.cynexus.ai.common.constant;

public final class AiConstants {

    public static final String DEFAULT_SCENARIO = "market_daily_review";
    public static final String AUTONOMOUS_AGENT_CAPABILITY = "autonomous_agent";
    public static final String WORKSPACE_CHAT_CAPABILITY = "workspace_chat";
    public static final String TEXT_SUMMARY_CAPABILITY = "text_summary";
    public static final String REASONING_EFFORT_PARAM = "reasoning_effort";
    public static final String MODEL_REASONING_EFFORT_EXTRA = "model_reasoning_effort";
    public static final String REASONING_SUMMARY_EXTRA = "reasoning_summary";
    public static final String REASONING_EFFORT_REQUEST_FIELD = "reasoning_effort";
    public static final String DEFAULT_REASONING_EFFORT = "low";
    public static final int MAX_LOG_TEXT_LENGTH = 8_000;
    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_ARCHIVED = "archived";
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_MARKDOWN = "markdown";

    private AiConstants() {
    }
}
