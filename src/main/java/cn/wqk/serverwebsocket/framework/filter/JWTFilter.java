package cn.wqk.serverwebsocket.framework.filter;

import cn.wqk.serverwebsocket.utils.JWTUtil;
import cn.wqk.serverwebsocket.utils.RedisUtil;
import cn.wqk.serverwebsocket.utils.ServletUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 过滤器（含全局 CORS 支持 + 白名单放行）
 */
@Component
@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    // ✅ 不拦截的路径列表（登录注册、公开接口等）
    private static final List<String> WHITELIST = Arrays.asList(
            "/user/login",
            "/user/register",
            "/user/path",
            "/favicon.ico",
            "/doc.html",
            "/garbage/recognize"
    );

    // 依赖注入 RedisUtil (推荐使用构造函数注入)
    private final RedisUtil redisUtil;

    public JWTFilter(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // --- 1. 处理 CORS 和 OPTIONS 请求 ---
        addCorsHeaders(response);

        // 1.1. 放行所有 OPTIONS 请求（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // --- 2. 白名单路径放行 ---
        String uri = request.getRequestURI();
        log.debug("Processing URI: {}", uri);

        if (isWhitelisted(uri)
                || uri.startsWith("/file")
                || uri.startsWith("/websocket")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3")
                || uri.startsWith("/webjars")
                || uri.startsWith("/test")
        ) {
            log.debug("URI {} matched whitelist or static resources, skipping JWT check.", uri);
            filterChain.doFilter(request, response);
            return;
        }

        // --- 3. JWT 校验逻辑 ---
        String authHeader = request.getHeader("Authorization");

        // 3.1. 检查 Authorization 头是否存在
        if (authHeader == null || authHeader.isEmpty()) {
            log.warn("Access denied: Missing Authorization header for URI: {}", uri);
            ServletUtil.renderString(response, "缺少 Authorization 头", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 3.2. 检查是否为 Bearer 格式并提取 token
        String token;
        if (authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length()).trim();
        } else {
            log.warn("Access denied: Invalid Authorization header format for URI: {}", uri);
            ServletUtil.renderString(response, "Authorization 头格式不正确，应为 Bearer <token>", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 3.3. 校验 JWT 自身的有效性（签名、过期时间等）
        if (!JWTUtil.verifyToken(token)) {
            log.warn("Access denied: Token invalid or expired for URI: {}", uri);
            ServletUtil.renderString(response, "Token 无效或过期", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 3.4. 校验 Redis 中的登录状态 (防止被盗用或单点登录冲突)
        int userId = JWTUtil.getUserId(token);
        String redisToken = redisUtil.get(String.valueOf(userId));

        if (redisToken == null) {
            log.warn("Access denied: Login expired (Redis token not found) for UserID: {}", userId);
            ServletUtil.renderString(response, "登录已过期，请重新登录", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!redisToken.equals(token)) {
            log.warn("Access denied: Token mismatch (Redis token != Request token) for UserID: {}", userId);
            ServletUtil.renderString(response, "您的账号已在其他地方登录，或请勿伪造token", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 3.5. 校验通过，存入 request 属性
        String username = JWTUtil.getUsername(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        log.info("JWT check passed for User: {} (ID: {}) on URI: {}", username, userId, uri);


        // --- 4. 一切正常，放行 ---
        filterChain.doFilter(request, response);
    }

    /**
     * 判断路径是否在白名单中
     */
    private boolean isWhitelisted(String uri) {
        return WHITELIST.stream().anyMatch(uri::startsWith);
    }

    /**
     * ✅ 统一添加 CORS 响应头
     * 注意：对于复杂的跨域需求，建议使用 Spring WebMvcConfigurer 进行全局配置。
     */
    private void addCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:8080");
        // 允许所有必要的 HTTP 方法
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");
        // 允许客户端携带的自定义头
        response.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        // 预检请求（OPTIONS）的缓存时间
        response.setHeader("Access-Control-Max-Age", "3600");
    }
}