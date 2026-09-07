package cn.wyq.serverwebsocket.interceptor;

import cn.wyq.serverwebsocket.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.entity.UserEmailApplyEntity;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.util.JWTUtil;
import cn.wyq.serverwebsocket.utils.BaseContext;
import cn.wyq.serverwebsocket.utils.ServletUtil;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
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
/**
 * JWTInterceptor：请求拦截组件，负责请求前后的通用处理。
 */
@Slf4j
@Component
public class JWTInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    @jakarta.annotation.Resource
    private SysConfigService sysConfigService;

    @jakarta.annotation.Resource
    private UserMapper userMapper;

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

        // 3.4 仅允许 access token，刷新令牌不能作为受保护接口的登录凭据。
        if (!JWTUtil.verifyAccessToken(token)) {
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

            // 维护模式下保留认证、配置和管理员操作，其余用户请求统一返回可识别的 503。
            if (isMaintenanceMode() && !isMaintenanceExempt(request.getRequestURI())
                    && !isAdministrator(userId)) {
                ServletUtil.renderString(response, maintenanceMessage(),
                        HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return false;
            }

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

    private boolean isMaintenanceMode() {
        try {
            return SystemConfigUtils.booleanValue(sysConfigService.getConfigMap(),
                    "maintenance_mode", false);
        } catch (RuntimeException exception) {
            log.warn("读取维护模式配置失败，将按正常模式处理", exception);
            return false;
        }
    }

    private String maintenanceMessage() {
        try {
            String message = SystemConfigUtils.stringValue(sysConfigService.getConfigMap(),
                    "maintenance_message", "系统维护中，请稍后再试。");
            return message.length() > 500 ? message.substring(0, 500) : message;
        } catch (RuntimeException exception) {
            return "系统维护中，请稍后再试。";
        }
    }

    private boolean isMaintenanceExempt(String requestUri) {
        if (requestUri == null) {
            return false;
        }
        return requestUri.startsWith("/sys-config/")
                || requestUri.startsWith("/ai/admin/")
                || requestUri.equals("/user/logout")
                || requestUri.equals("/user/refresh");
    }

    private boolean isAdministrator(int userId) {
        try {
            UserEmailApplyEntity user = userMapper.getCurrentUserById(userId);
            return user != null && Integer.valueOf(1).equals(user.getStatus())
                    && (user.getRole() == 0 || user.getRole() == 1);
        } catch (RuntimeException exception) {
            log.warn("维护模式下读取当前用户角色失败，将拒绝普通请求", exception);
            return false;
        }
    }
}
