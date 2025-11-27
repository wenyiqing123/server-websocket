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

    // ✅ 不拦截的路径列表（登录注册、公开接口等）
    private static final List<String> WHITELIST = Arrays.asList(
            "/user/login",
            "/user/register",
            "/user/path",
            "/favicon.ico",
            "/doc.html",
            "/garbage/recognize"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ✅ 每次请求都加上 CORS 响应头（无论是否校验通过）
        addCorsHeaders(response);

        // ✅ 1. 放行所有 OPTIONS 请求（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // ✅ 2. 白名单路径或静态资源直接放行
        String uri = request.getRequestURI();
        log.info("uri = {}", uri);
        if (isWhitelisted(uri)
                || uri.startsWith("/file")
                || uri.startsWith("/websocket")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3")
                || uri.startsWith("/webjars")
                || uri.startsWith("/test")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ 3. 处理 JWT 校验
        String authHeader = request.getHeader("Authorization");
        String token = null;

        //&& authHeader.startsWith("Bearer ")
        if (authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (authHeader != null) {
            token = authHeader;
            if (JWTUtil.verifyToken(token)) {
                int userId = JWTUtil.getUserId(token);
                RedisUtil redisUtil = new RedisUtil();
                String redisToken = redisUtil.get(String.valueOf(userId));

                if (redisToken == null) {
                    ServletUtil.renderString(response, "登录已过期", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                if (!redisToken.equals(token)) {
                    ServletUtil.renderString(response, "请勿伪造token（携带token与登录token不一致）", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                // ✅ 校验通过，存入 request 属性
                String username = JWTUtil.getUsername(token);
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);

            } else {
                ServletUtil.renderString(response, "Token 无效或过期", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

        } else {
            ServletUtil.renderString(response, "缺少 Authorization 头", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // ✅ 4. 一切正常，放行
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
     */
    private void addCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:8080");
//        response.setHeader("Access-Control-Allow-Origin", "http://localhost:8000");
        //前端打包后的本地运行ip
//        response.setHeader("Access-Control-Allow-Origin", "http://127.0.0.1:5500");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH"); // ✅ 加上 PATCH
        response.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

}
