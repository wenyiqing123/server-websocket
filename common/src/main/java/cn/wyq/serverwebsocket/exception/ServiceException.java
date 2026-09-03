package cn.wyq.serverwebsocket.exception;

import lombok.Data;

/**
 * ServiceException：异常处理组件，用于统一表达或处理应用异常。
 */
@Data
public class ServiceException extends RuntimeException{
    private int code;

    public ServiceException(String message, int code) {
        super(message);
        this.code = code;
    }
}
