package cn.wqk.serverwebsocket.framework.common;

import java.io.Serializable;
import java.util.HashMap;


public class Result extends HashMap<String, Object> implements Serializable {
    private static final int SUCCESS_CODE = 200;
    private static final String SUCCESS_MSG = "操作成功";
    private static final int ERROR_CODE = 400;
    private static final String ERROR_MSG = "操作失败";
    private static final long serialVersionUID=1881899354058984967L;

    public Result() {
    }

    public Result(int code, String msg, Object data) {
        this.put("code", code);
        this.put("msg", msg);
        if (data != null) {
            this.put("data", data);
        }
    }

    public static Result success(String msg, Object data) {
        return new Result(SUCCESS_CODE, msg, data);
    }

    public static Result success(String msg) {
        return success(msg, null);
    }

    public static <T> Result success(T data) {
        return success(SUCCESS_MSG, data);
    }

    public static Result success() {
        return success(SUCCESS_MSG);
    }

    public static Result error(int code, String msg) {
        return new Result(code, msg, null);
    }

    public static Result error(String msg) {
        return error(ERROR_CODE, msg);
    }

    public static Result error(int code) {
        return error(code, ERROR_MSG);
    }

    public static Result error() {
        return error(ERROR_MSG);
    }

    public static Result toToken(String token) {
        return new Result().put("code", 200).put("msg", "登录成功").put("token", token);
    }

    /**
     * 对service返回的受影响行数i做判断
     *
     * @param i
     * @param msg
     * @return Result
     */
    public static Result toAjax(int i, int code, String msg) {
        if (i > 0)
            return success();
        if (msg != null)
            return error(code, msg);
        return error();
    }


    @Override
    public Result put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
