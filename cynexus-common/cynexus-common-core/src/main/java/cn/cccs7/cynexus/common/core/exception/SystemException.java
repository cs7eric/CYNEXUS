package cn.cccs7.cynexus.common.core.exception;

import lombok.Getter;

@Getter
public class SystemException extends RuntimeException {

    private final String code;

    public SystemException(String message) {
        super(message);
        this.code = "100003";
    }

    public SystemException(String code, String message) {
        super(message);
        this.code = code;
    }
}
