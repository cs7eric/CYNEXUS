package cn.cccs7.cynexus.common.core.result;

import cn.cccs7.cynexus.common.core.constant.TraceId;
import lombok.Getter;
import java.io.Serializable;

@Getter
public class Result<T> implements Serializable {

    private String code;
    private String message;
    private T data;
    private boolean success;
    private Long timestamp;
    private String traceId;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = ResultCode.SUCCESS.getCode();
        result.message = ResultCode.SUCCESS.getMessage();
        result.data = data;
        result.success = true;
        result.timestamp = System.currentTimeMillis();
        result.traceId = TraceId.current();
        return result;
    }

    public static <T> Result<T> fail(String code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.success = false;
        result.timestamp = System.currentTimeMillis();
        result.traceId = TraceId.current();
        return result;
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMessage());
    }

    public boolean isFailed() {
        return !success;
    }
}
