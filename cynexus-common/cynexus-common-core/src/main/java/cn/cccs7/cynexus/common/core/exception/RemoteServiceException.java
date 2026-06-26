package cn.cccs7.cynexus.common.core.exception;

import lombok.Getter;

@Getter
public class RemoteServiceException extends RuntimeException {

    private final String code;

    public RemoteServiceException(String message) {
        super(message);
        this.code = "200001";
    }
}
