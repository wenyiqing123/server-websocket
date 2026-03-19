package cn.wyq.serverwebsocket.interceptor;

import cn.wyq.serverwebsocket.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.util.JWTUtil;
import cn.wyq.serverwebsocket.utils.BaseContext;
import cn.wyq.serverwebsocket.utils.ServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * 权限验证拦截器 (纯无状态 Access Token 极致性能版)
 */
@Slf4j
@Component
public class JWTInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    // 💡 架构优化点：因为 Access Token 不再查 Redis，
    // 这里彻底移除了 RedisUtil 的依赖，类结构更干净，拦截速度达到极致！

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 如果不是映射到方法直接通过 (例如静态资源等)
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

        // 3.1 检查 OPTIONS 请求 (放行预检请求，防止跨域拦截报错)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 3.2 获取 Authorization 头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            ServletUtil.renderString(response, "缺少 Authorization 头", HttpServletResponse.SC_UNAUTHORIZED);
            return false; // 拦截
        }

        // 3.3 检查 Bearer 格式并提取出纯净的 Token 字符串
        String token;
        if (authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length()).trim();
        } else {
            ServletUtil.renderString(response, "Authorization 格式错误", HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 3.4 🚀 性能核心：完全依靠 CPU 校验 Token 有效性 (签名防伪篡改 + exp 判断过期)
        if (!JWTUtil.verifyToken(token)) {
            ServletUtil.renderString(response, "Token 无效或已过期", HttpServletResponse.SC_UNAUTHORIZED);
            return false; // 此时前端应该收到 401 状态码，并自动触发无感刷新逻辑
        }

        // --- 删除了原本 3.6 查 Redis 的逻辑 ---

        // 3.5 校验通过，安全地从 Token 载荷 (Payload) 中提取用户信息
        try {
            int userId = JWTUtil.getUserId(token);
            String username = JWTUtil.getUsername(token);

            // 存入 Request 作用域，方便 Controller 中直接获取 (如果需要的话)
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);

            // 存入 ThreadLocal，方便 Service 层等任何地方随时获取当前登录用户 ID
            BaseContext.setCurrentId((long) userId);

            return true; // 极速放行！
        } catch (Exception e) {
            log.error("从合法的 Token 中提取信息时发生异常", e);
            ServletUtil.renderString(response, "Token 数据解析异常", HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 🧹 极其重要：请求处理完毕返回给前端之前，务必清理 ThreadLocal。
        // 因为 Tomcat 使用线程池，如果不清理，下一个复用该线程的用户可能会读到上一个用户的数据（串号风险）！
        BaseContext.removeCurrentId();
    }
}