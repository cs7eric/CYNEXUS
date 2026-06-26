package cn.cccs7.cynexus.common.core.exception;

import cn.cccs7.cynexus.common.core.enums.BaseEnum;

public enum ErrorCode implements BaseEnum<String> {
    SUCCESS("0", "success"),
    BAD_REQUEST("400", "bad request"),
    NOT_FOUND("404", "not found"),
    INTERNAL_ERROR("500", "internal error");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
