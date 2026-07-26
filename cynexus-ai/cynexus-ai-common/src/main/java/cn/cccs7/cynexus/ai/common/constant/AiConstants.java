package cn.cccs7.cynexus.ai.common.constant;

public final class AiConstants {

    public static final String DEFAULT_SCENARIO = "market_daily_review";
    public static final String AUTONOMOUS_AGENT_CAPABILITY = "autonomous_agent";
    public static final String WORKSPACE_CHAT_CAPABILITY = "workspace_chat";
    public static final String TEXT_SUMMARY_CAPABILITY = "text_summary";
    public static final String REASONING_EFFORT_PARAM = "reasoning_effort";
    public static final String MODEL_REASONING_EFFORT_EXTRA = "model_reasoning_effort";
    public static final String REASONING_SUMMARY_EXTRA = "reasoning_summary";
    public static final String MODEL_WIRE_API_EXTRA = "wire_api";
    public static final String WIRE_API_RESPONSES = "responses";
    public static final String WIRE_API_CHAT_COMPLETIONS = "chat_completions";
    public static final String REASONING_EFFORT_REQUEST_FIELD = "reasoning_effort";
    public static final String DEFAULT_REASONING_EFFORT = "low";
    public static final String RESPONSES_TYPE_FIELD = "type";
    public static final String RESPONSES_FUNCTION_CALL_TYPE = "function_call";
    public static final String RESPONSES_FUNCTION_CALL_OUTPUT_TYPE = "function_call_output";
    public static final String RESPONSES_FUNCTION_CALL_ID_FIELD = "call_id";
    public static final String RESPONSES_FUNCTION_CALL_ITEM_ID_FIELD = "id";
    public static final String RESPONSES_FUNCTION_CALL_ARGUMENTS_FIELD = "arguments";
    public static final String RESPONSES_FUNCTION_CALL_NAME_FIELD = "name";
    public static final String RESPONSES_FUNCTION_CALL_OUTPUT_FIELD = "output";
    public static final String PROMPT_CACHE_KEY_VERSION = "v3";
    public static final String PROMPT_CACHE_CAPABILITY = AUTONOMOUS_AGENT_CAPABILITY;
    public static final String PROMPT_CACHE_XAI_CONVERSATION_HEADER = "x-grok-conv-id";
    public static final String PROMPT_CACHE_KEY_FIELD = "prompt_cache_key";
    public static final String AI_USAGE_FIELD = "usage";
    public static final String PROMPT_CACHE_USAGE_PRIMARY_DETAILS_FIELD = "prompt_tokens_details";
    public static final String PROMPT_CACHE_USAGE_ALTERNATIVE_DETAILS_FIELD = "input_tokens_details";
    public static final String PROMPT_CACHE_USAGE_CACHED_TOKENS_FIELD = "cached_tokens";
    public static final String PROMPT_CACHE_USAGE_READ_TOKENS_FIELD = "cache_read_input_tokens";
    public static final String PROMPT_CACHE_USAGE_HIT_TOKENS_FIELD = "prompt_cache_hit_tokens";
    public static final String PROMPT_CACHE_USAGE_WRITE_TOKENS_FIELD = "cache_write_tokens";
    public static final String PROMPT_CACHE_USAGE_CREATION_TOKENS_FIELD = "cache_creation_input_tokens";
    public static final String PROMPT_CACHE_USAGE_TOTAL_CACHED_TOKENS_FIELD = "total_cached_tokens";
    public static final String PROMPT_CACHE_USAGE_CACHED_INPUT_TOKENS_FIELD = "cached_input_tokens";
    public static final String TOOL_CHOICE_FIELD = "tool_choice";
    public static final String TOOL_CHOICE_AUTO = "auto";
    public static final String TOOL_CHOICE_NONE = "none";
    public static final String AI_USAGE_INPUT_TOKENS_FIELD = "input_tokens";
    public static final String AI_USAGE_PROMPT_TOKENS_FIELD = "prompt_tokens";
    public static final String AI_USAGE_OUTPUT_TOKENS_FIELD = "output_tokens";
    public static final String AI_USAGE_COMPLETION_TOKENS_FIELD = "completion_tokens";
    public static final String AI_USAGE_TOTAL_TOKENS_FIELD = "total_tokens";
    public static final int MAX_LOG_TEXT_LENGTH = 8_000;
    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_ARCHIVED = "archived";
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_MARKDOWN = "markdown";

    private AiConstants() {
    }
}
