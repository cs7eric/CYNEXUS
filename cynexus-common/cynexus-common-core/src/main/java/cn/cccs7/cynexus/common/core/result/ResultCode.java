package cn.cccs7.cynexus.common.core.result;

public enum ResultCode {
    SUCCESS("0", "success"),
    BAD_REQUEST("400", "bad request"),
    UNAUTHORIZED("401", "unauthorized"),
    FORBIDDEN("403", "forbidden"),
    NOT_FOUND("404", "not found"),
    VALIDATION_ERROR("100001", "validation error"),
    BUSINESS_ERROR("100002", "business error"),
    SYSTEM_ERROR("100003", "system error"),
    REMOTE_SERVICE_ERROR("200001", "remote service error"),
    AI_MODEL_ERROR("300001", "ai model error"),
    AI_TOOL_ERROR("300002", "ai tool error"),
    AI_OUTPUT_PARSE_ERROR("300003", "ai output parse error");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
