package cn.cccs7.cynexus.common.web.advice;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.RemoteServiceException;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.common.core.result.ResultCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(RemoteServiceException.class)
    public Result<Void> handleRemoteServiceException(RemoteServiceException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), exception.getMessage());
    }
}
