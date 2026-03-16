package cn.wyq.serverwebsocket.constant;

public class ResultConstant {
    // ===================== 成功类 =====================
    public static final int SUCCESS = 200;
    public static final String SUCCESS_MSG = "success";

    public static final int DefaultEroor = 400;
    public static final String DefaultEroor_MSG = "error";
    // ===================== 客户端错误 =====================
    public static final int BAD_REQUEST = 400;
    public static final String BAD_REQUEST_MSG = "请求参数错误";
    public static final int UNAUTHORIZED = 401;
    public static final String UNAUTHORIZED_MSG = "未授权";
    public static final int FORBIDDEN = 403;
    public static final String FORBIDDEN_MSG = "无访问权限";
    public static final int NOT_FOUND = 404;
    public static final String NOT_FOUND_MSG = "请求路径不存在";
    // ===================== 服务端错误 =====================
    public static final int SERVER_ERROR = 500;
    public static final String SERVER_ERROR_MSG = "服务器内部错误";
    public static final int SERVICE_UNAVAILABLE = 503;
    public static final String SERVICE_UNAVAILABLE_MSG = "服务暂不可用";
    // ===================== 自定义业务状态 =====================
    public static final int VALIDATE_FAILED = 1001;
    public static final String VALIDATE_FAILED_MSG = "参数校验失败";
    public static final int BUSINESS_ERROR = 1002;
    public static final String BUSINESS_ERROR_MSG = "业务逻辑异常";
    public static final int DATA_NOT_EXIST = 1003;
    public static final String DATA_NOT_EXIST_MSG = "数据不存在";

    // 防止被实例化
    private ResultConstant() {
    }
}
