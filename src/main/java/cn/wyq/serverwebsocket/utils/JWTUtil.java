package cn.wyq.serverwebsocket.utils;

import cn.wyq.serverwebsocket.pojo.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;

/**
 * JWT 工具类
 * 用于生成、校验、解析 Token
 * <p>
 * 依赖库：com.auth0:java-jwt
 */
public class JWTUtil {

    /**
     * 签名密钥（必须保密，不要硬编码到代码里）
     * 建议放到 application.yml 或环境变量中
     */
    private static final String SECRET = "secret_for_jwt2";

    /**
     * Token 过期时间（1小时 = 3600_000 ms）
     * 实际项目里可以配置成 15分钟 + RefreshToken 机制
     */
    private static final long EXPIRE_TIME = 1000 * 60 * 60;

    /**
     * 生成 JWT Token
     *
     * @param user 用户对象（只存储必要的字段，不要存密码）
     * @return token 字符串
     */
    public static String generateToken(User user) {
        return JWT.create()
                // JWT 的 subject 一般用来存放用户唯一标识，这里用 userId
                .withSubject(String.valueOf(user.getId()))

                // 自定义 claim，可以存放用户名、角色等信息（不要放敏感数据，比如密码）
                .withClaim("username", user.getUserName())

                // 过期时间
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME))

                // 使用 HMAC256 算法签名（对称加密：同一个密钥签发和校验）
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 校验 Token 并返回解析后的 JWT 对象（辅助其他方法的使用，不直接返回给客户端）
     *
     * @param token 客户端传入的 token
     * @return 解析后的 JWT（包含 payload 信息）
     * @throws com.auth0.jwt.exceptions.JWTVerificationException 如果 token 非法或过期
     */
    private static DecodedJWT verify(String token) {
        return JWT.require(Algorithm.HMAC256(SECRET))
                .build()
                .verify(token);
    }

    /**
     * 从 Token 中获取用户 ID
     *
     * @param token token
     * @return userId
     */
    public static int getUserId(String token) {
        return Integer.parseInt(verify(token).getSubject());
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token token
     * @return 用户名
     */
    public static String getUsername(String token) {
        return verify(token).getClaim("username").asString();
    }

    /**
     * 校验 Token 是否有效（合法且未过期）
     *
     * @param token token
     * @return true = 有效，false = 无效
     */
    public static boolean verifyToken(String token) {
        try {
            verify(token);
            return true;
        } catch (Exception e) {
            // token 非法或过期
            return false;
        }
    }
}
