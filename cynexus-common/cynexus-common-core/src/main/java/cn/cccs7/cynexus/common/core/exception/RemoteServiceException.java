package cn.cccs7.cynexus.common.core.exception;

public class RemoteServiceException extends RuntimeException {

    private final String code;

    public RemoteServiceException(String message) {
        super(message);
        this.code = "200001";
    }

    public String getCode() {
        return code;
    }
}
