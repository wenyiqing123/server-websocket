package cn.wyq.serverwebsocket.util;


import cn.wyq.serverwebsocket.pojo.User;
/**
 * JWT 工具类
 *
 * <p>Web 模块保留此兼容入口，实际签发与校验统一委托给 common 模块，
 * 防止两套实现使用不同的签名密钥或校验规则。</p>
 */
public class JWTUtil {

    private JWTUtil() {
    }

    /**
     * 生成 JWT Token
     *
     * @param user 用户对象（只存储必要的字段，不要存密码）
     * @return token 字符串
     */
    public static String generateToken(User user) {
        return cn.wyq.serverwebsocket.utils.JWTUtil.generateToken(user.getId(), user.getUserName());
    }
    public static String generateRefreshToken(User user) {
        return cn.wyq.serverwebsocket.utils.JWTUtil.generateRefreshToken(user.getId(), user.getUserName());
    }

    /**
     * 从 Token 中获取用户 ID
     *
     * @param token token
     * @return userId
     */
    public static int getUserId(String token) {
        return cn.wyq.serverwebsocket.utils.JWTUtil.getUserId(token);
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token token
     * @return 用户名
     */
    public static String getUsername(String token) {
        return cn.wyq.serverwebsocket.utils.JWTUtil.getUsername(token);
    }

    /**
     * 校验 Token 是否有效（合法且未过期）
     *
     * @param token token
     * @return true = 有效，false = 无效
     */
    public static boolean verifyToken(String token) {
        return cn.wyq.serverwebsocket.utils.JWTUtil.verifyToken(token);
    }

    /**
     * 校验可用于受保护接口与 WebSocket 的 access token。
     *
     * @param token access token
     * @return true 表示令牌有效且用途正确
     */
    public static boolean verifyAccessToken(String token) {
        return cn.wyq.serverwebsocket.utils.JWTUtil.verifyAccessToken(token);
    }
}
