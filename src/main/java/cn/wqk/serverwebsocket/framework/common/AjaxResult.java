package cn.wqk.serverwebsocket.framework.common;

import cn.wqk.serverwebsocket.framework.constant.ResultConstant;
import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class AjaxResult<T> implements Serializable {
    // 状态码常量
    public static final int SUCCESS_CODE = 200;
    public static final int ERROR_CODE = 500;
    // 默认消息
    public static final String SUCCESS_MSG = "success";
    public static final String ERROR_MSG = "error";
    private static final long serialVersionUID = 1L;
    private Integer code;
    private String message;
    private T data;

    // ✅ success 方法（四种重载形式）
    public static <T> AjaxResult<T> success(String msg, T data) {
        return new AjaxResult<>(ResultConstant.SUCCESS, msg, data);
    }

    public static <T> AjaxResult<T> success(String msg) {
        return success(msg, null);
    }

    public static <T> AjaxResult<T> success(T data) {
        return success(ResultConstant.SUCCESS_MSG, data);
    }

    public static <T> AjaxResult<T> success() {
        return success(ResultConstant.SUCCESS_MSG, null);
    }

    // ❌ error 方法（四种重载形式）
    public static <T> AjaxResult<T> error(int code, String msg, T data) {
        return new AjaxResult<>(code, msg, data);
    }

    public static <T> AjaxResult<T> error(String msg, T data) {
        return error(ResultConstant.DefaultEroor, msg, data);
    }

    public static <T> AjaxResult<T> error(String msg) {
        return error(ResultConstant.DefaultEroor, msg, null);
    }

    public static <T> AjaxResult<T> error() {
        return error(ResultConstant.DefaultEroor, ResultConstant.DefaultEroor_MSG, null);
    }

}
