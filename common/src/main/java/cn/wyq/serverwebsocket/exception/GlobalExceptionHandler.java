package cn.wyq.serverwebsocket.exception;


import cn.wyq.serverwebsocket.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.net.ssl.SSLHandshakeException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ServiceException.class)
    public Result serviceException(ServiceException e) {
        return Result.error(e.getCode(), e.getMessage());
    }


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result MaxUploadSizeExceededException(ServiceException e) {
        return Result.error(e.getCode(), "上传文件大小超出限制，最大可上传1MB");
    }

    @ExceptionHandler(SSLHandshakeException.class)
    public Result SSLHandshakeException(ServiceException e) {
        return Result.error(e.getCode(), "宝宝，你的网络链接似乎有问题哦~");
    }
}
