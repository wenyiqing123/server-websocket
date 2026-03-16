package cn.wyq.serverwebsocket.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;

/**
 * JWT 工具类
 * 用于生成、校验、解析 Token
 * <p>
 * 依赖库：com.auth0:java-jwt
 * 架构说明：已与业务实体(Pojo)完全解耦，安全放置于 common 模块
 */
public class JWTUtil {

    /**
     * 签名密钥（必须保密，不要硬编码到代码里）
     * 建议放到 application.yml 或环境变量中
     */
    private static final String SECRET = "secret_for_jwt2";

    /**
     * Token 过期时间（30min）
     */
    private static final long EXPIRE_TIME = 1000 * 60 * 30;
    private static final long REFRESH_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7;

    /**
     * 生成 JWT Token
     * 💡 架构优化：不再接收 User 对象，只接收具体的必要参数，彻底解耦
     *
     * @param userId   用户唯一标识
     * @param username 用户名
     * @return token 字符串
     */
    public static String generateToken(Integer userId, String username) {
        return JWT.create()
                // JWT 的 subject 一般用来存放用户唯一标识
                .withSubject(String.valueOf(userId))

                // 自定义 claim，存放用户名、角色等信息（不要放敏感数据）
                .withClaim("username", username)

                // 过期时间
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME))

                // 使用 HMAC256 算法签名
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 生成 刷新用 JWT Token
     * 💡 架构优化：同样改为接收基础数据类型的参数
     *
     * @param userId   用户唯一标识
     * @param username 用户名
     * @return token 字符串
     */
    public static String generateRefreshToken(Integer userId, String username) {
        return JWT.create()
                // JWT 的 subject 一般用来存放用户唯一标识
                .withSubject(String.valueOf(userId))

                // 自定义 claim，存放用户名
                .withClaim("username", username)

                // 过期时间
                .withExpiresAt(new Date(System.currentTimeMillis() + REFRESH_EXPIRE_TIME))

                // 使用 HMAC256 算法签名
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