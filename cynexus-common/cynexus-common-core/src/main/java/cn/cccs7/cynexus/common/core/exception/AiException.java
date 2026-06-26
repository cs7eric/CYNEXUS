package cn.cccs7.cynexus.common.core.exception;

import lombok.Getter;

@Getter
public class AiException extends RuntimeException {

    private final String code;

    public AiException(String code, String message) {
        super(message);
        this.code = code;
    }
}
