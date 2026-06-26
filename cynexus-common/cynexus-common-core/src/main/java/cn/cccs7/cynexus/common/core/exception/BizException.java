package cn.cccs7.cynexus.common.core.exception;

public class BizException extends RuntimeException {

    private final String code;

    public BizException(String message) {
        super(message);
        this.code = "100002";
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode.message());
        this.code = errorCode.code();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.code();
    }

    public String getCode() {
        return code;
    }
}
