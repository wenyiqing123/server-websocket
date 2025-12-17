package cn.wyq.serverwebsocket.framework.exception;

import lombok.Data;

@Data
public class ServiceException extends RuntimeException{
    private int code;

    public ServiceException(String message, int code) {
        super(message);
        this.code = code;
    }
}
