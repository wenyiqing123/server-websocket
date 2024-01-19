package cn.wqk.serverwebsocket.exception;

import cn.wqk.serverwebsocket.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ServiceException.class)
    public Result serviceException(ServiceException e){
        return Result.error(e.getCode(),e.getMessage());
    }
}
