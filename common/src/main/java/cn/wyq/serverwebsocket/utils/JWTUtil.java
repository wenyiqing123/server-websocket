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
     * 由 Web 模块启动时从安全配置注入的签名算法。
     *
     * <p>公共模块不能直接读取 Spring 配置，因此不保留任何固定默认密钥。
     * Web 模块启动时必须显式调用 {@link #configureSecret(String)}；本地临时密钥也由
     * Web 配置层随机生成后注入。</p>
     */
    private static volatile Algorithm signingAlgorithm;

    /** 默认 Access Token 有效期（30 分钟），启动时可由平台配置覆盖。 */
    private static final long DEFAULT_EXPIRE_TIME = 1000L * 60 * 30;
    /** 默认 Refresh Token 有效期（7 天），启动时可由平台配置覆盖。 */
    private static final long DEFAULT_REFRESH_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 7;
    private static volatile long expireTime = DEFAULT_EXPIRE_TIME;
    private static volatile long refreshExpireTime = DEFAULT_REFRESH_EXPIRE_TIME;
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    /**
     * 配置 JWT 签名密钥。
     *
     * <p>仅由启动配置调用；密钥不会被记录到日志或返回给调用方。</p>
     *
     * @param secret 从受控配置或环境变量读取的密钥
     */
    public static void configureSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT signing secret must not be blank");
        }
        signingAlgorithm = Algorithm.HMAC256(secret);
    }

    /**
     * 配置令牌有效期。单位分别为分钟和天，供 Web 层在启动及签发令牌前调用。
     * 使用 long 计算并进行溢出保护，避免错误配置得到已经过期的令牌。
     */
    public static void configureTokenExpiry(long accessMinutes, long refreshDays) {
        if (accessMinutes <= 0 || refreshDays <= 0) {
            throw new IllegalArgumentException("JWT token expiry must be positive");
        }
        long accessMillis;
        long refreshMillis;
        try {
            accessMillis = Math.multiplyExact(Math.multiplyExact(accessMinutes, 60_000L), 1L);
            refreshMillis = Math.multiplyExact(Math.multiplyExact(refreshDays, 86_400_000L), 1L);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("JWT token expiry is too large", exception);
        }
        expireTime = accessMillis;
        refreshExpireTime = refreshMillis;
    }

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

                // access token 只能用于受保护接口和 WebSocket 握手
                .withClaim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)

                // 过期时间
                .withExpiresAt(new Date(System.currentTimeMillis() + expireTime))

                // 使用 HMAC256 算法签名
                .sign(getSigningAlgorithm());
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

                // refresh token 仅用于换取新的令牌对
                .withClaim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)

                // 过期时间
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshExpireTime))

                // 使用 HMAC256 算法签名
                .sign(getSigningAlgorithm());
    }

    /**
     * 校验 Token 并返回解析后的 JWT 对象（辅助其他方法的使用，不直接返回给客户端）
     *
     * @param token 客户端传入的 token
     * @return 解析后的 JWT（包含 payload 信息）
     * @throws com.auth0.jwt.exceptions.JWTVerificationException 如果 token 非法或过期
     */
    private static DecodedJWT verify(String token) {
        return JWT.require(getSigningAlgorithm())
                .build()
                .verify(token);
    }

    /**
     * 获取当前已注入的签名算法。
     *
     * @return 已配置的签名算法
     */
    private static Algorithm getSigningAlgorithm() {
        Algorithm algorithm = signingAlgorithm;
        if (algorithm == null) {
            throw new IllegalStateException("JWT signing secret has not been configured");
        }
        return algorithm;
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

    /**
     * 校验 access token。无用途标记的旧 Token 不可用于受保护接口，
     * 防止历史 refresh token 被当作长效登录凭据使用。
     *
     * @param token access token
     * @return true 表示签名、过期时间和用途均有效
     */
    public static boolean verifyAccessToken(String token) {
        return verifyTokenType(token, ACCESS_TOKEN_TYPE, false);
    }

    /**
     * 校验 refresh token。为平滑升级，短期兼容旧版本的无用途标记 Token；
     * 旧 Token 本身最长七天过期，之后只接受显式 refresh 标记。
     *
     * @param token refresh token
     * @return true 表示签名、过期时间和用途有效
     */
    public static boolean verifyRefreshToken(String token) {
        return verifyTokenType(token, REFRESH_TOKEN_TYPE, true);
    }

    private static boolean verifyTokenType(String token, String expectedType, boolean allowLegacyToken) {
        try {
            String tokenType = verify(token).getClaim(TOKEN_TYPE_CLAIM).asString();
            return expectedType.equals(tokenType)
                    || (allowLegacyToken && (tokenType == null || tokenType.isBlank()));
        } catch (Exception exception) {
            return false;
        }
    }
}
