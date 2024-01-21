package cn.wqk.serverwebsocket.filter;


import cn.wqk.serverwebsocket.annotation.LoginNotRequired;
import cn.wqk.serverwebsocket.impl.LoginUser;
import cn.wqk.serverwebsocket.mapper.UserMapper;
import cn.wqk.serverwebsocket.pojo.User;
import cn.wqk.serverwebsocket.utils.JWTUtil;
import cn.wqk.serverwebsocket.utils.RedisUtil;
import cn.wqk.serverwebsocket.utils.ServletUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * 自定义的过滤器，用于处理请求中的Token验证逻辑
 */
@Component
public class TokenFilter extends OncePerRequestFilter {

    @Autowired
    private UserMapper userMapper;

    /**
     * 过滤器的核心逻辑，在请求进入Servlet容器之前进行拦截和处理
     *
     * @param request     HTTP请求对象
     * @param response    HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException 如果发生Servlet异常
     * @throws IOException      如果发生I/O异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 从请求头中获取Token
        String Authorization = request.getHeader("Authorization");
        // 根据shouldFilter方法判断是否需要进行过滤
        if (shouldFilter(request)) {
            // 如果Token为空，抛出未认证异常
            if (Authorization == null || Authorization.equals("")) {
                ServletUtil.renderString(response, "请登录", 401);
                return;
            }
            RedisUtil redisUtil = new RedisUtil();
            int userId = 0;
            try {
                // 解码Token获取用户ID
                userId = JWTUtil.decodeToken(Authorization);
            } catch (Exception e) {
                // Token解码失败，抛出未认证异常
                ServletUtil.renderString(response, "请勿伪造token(解析token失败)", 401);
                return;
            }
            //通过userId查询redis获取token
            String token = redisUtil.get(String.valueOf(userId));
            if (token == null) {
                ServletUtil.renderString(response, "登录已过期", 401);
                return;
            }
            if (!token.equals(Authorization)) {
                ServletUtil.renderString(response, "请勿伪造token(携带的token和登陆拿到的token不一致)", 401);
                return;
            }
            // 根据用户ID查询用户信息
            User user = userMapper.findById(userId);
            LoginUser loginUser = new LoginUser(user);
            // 如果用户为空，抛出未认证异常
            if (user == null) {
                ServletUtil.renderString(response, "请勿伪造token(解析成功，但userid是伪造的)", 401);
                return;
            }
            // 验证Token是否有效，如果无效则抛出未认证异常
            if (!JWTUtil.verifyToken(token)) {
                ServletUtil.renderString(response, "登录已过期", 401);
                return;
            }
            //将用户信息存入SecurityContextHolder中
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginUser, null, null);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            //将用户信息存放到HttpServletRequest中
            request.setAttribute("currentUser", user);
        }
        // 继续执行过滤器链中的下一个过滤器
        filterChain.doFilter(request, response);
    }


    /**
     * 根据实际需求检查请求路径是否需要进行拦截
     *
     * @param request HTTP请求对象
     * @return true表示需要拦截，false表示不需要拦截
     */
    private boolean shouldFilter(HttpServletRequest request) {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("/user/login");
        arrayList.add("/user/register");
        arrayList.add("/user/logout");
        arrayList.add("/websocket/**");
        String requestURI = request.getRequestURI();
        // 根据实际需求检查请求路径是否需要进行拦截
        for (String s : arrayList) {
            if (s.equals(requestURI) || requestURI.startsWith("/websocket")) {
                return false;
            }
        }
        return true;
    }

    public boolean isValid(HttpServletRequest request) {
        // 假设 HandlerMethod 作为属性存储在请求中
        HandlerMethod handlerMethod = (HandlerMethod) request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handlerMethod != null) {
            Method targetMethod = handlerMethod.getMethod();
            Annotation targetAnnotation = targetMethod.getAnnotation(LoginNotRequired.class);
            // 如果方法被标记为 LoginNotRequired，则返回 false
            return targetAnnotation == null;
        }
        // 如果 handlerMethod 不可用，继续进行过滤器链
        return false;
    }
}
