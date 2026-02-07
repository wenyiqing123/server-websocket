package cn.wyq.serverwebsocket.framework.interceptor;

import cn.wyq.serverwebsocket.framework.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.utils.BaseContext;
import cn.wyq.serverwebsocket.utils.JWTUtil;
import cn.wyq.serverwebsocket.utils.RedisUtil;
import cn.wyq.serverwebsocket.utils.ServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * 权限验证拦截器
 */
@Slf4j
@Component

public class JWTFilter implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private final RedisUtil redisUtil;

    public JWTFilter(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 如果不是映射到方法直接通过 (例如静态资源等，虽然一般会在 Config 中排除)
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 2. ✅ 核心：检查方法或类上是否有 @LoginNotRequired 注解
        if (method.isAnnotationPresent(LoginNotRequired.class) ||
                handlerMethod.getBeanType().isAnnotationPresent(LoginNotRequired.class)) {
            log.debug("接口 {} 标记为无需登录，直接放行", request.getRequestURI());
            return true;
        }

        // --- 3. 下面是原本 Filter 中的 JWT 校验逻辑 (直接搬过来) ---

        // 3.1 获取请求路径 (用于日志)
        String uri = request.getRequestURI();

        // 3.2 检查 OPTIONS 请求 (虽然一般会有 CORS Filter 处理，但这里兜底)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 3.3 获取 Authorization 头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            ServletUtil.renderString(response, "缺少 Authorization 头", HttpServletResponse.SC_UNAUTHORIZED);
            return false; // 拦截
        }

        // 3.4 检查 Bearer 格式
        String token;
        if (authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length()).trim();
        } else {
            ServletUtil.renderString(response, "Authorization 格式错误", HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 3.5 校验 Token 有效性
        if (!JWTUtil.verifyToken(token)) {
            ServletUtil.renderString(response, "Token 无效或过期", HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 3.6 校验 Redis (单点登录/踢人)
        int userId = JWTUtil.getUserId(token);
        String redisToken = redisUtil.get("accessToken:userId:" + userId);
        if (redisToken == null) {
            ServletUtil.renderString(response, "登录已过期", HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        if (!redisToken.equals(token)) {
            ServletUtil.renderString(response, "账号异地登录", HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 3.7 校验通过，存入用户信息到 Request
        String username = JWTUtil.getUsername(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        BaseContext.setCurrentId((long) userId);
        return true; // 放行
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.removeCurrentId();
    }
}