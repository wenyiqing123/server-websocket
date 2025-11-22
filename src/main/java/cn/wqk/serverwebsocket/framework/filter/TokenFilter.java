//package cn.wqk.serverwebsocket.framework.filter;
//
//
//import cn.wqk.serverwebsocket.framework.annotation.LoginNotRequired;
//import cn.wqk.serverwebsocket.mapper.UserMapper;
//import cn.wqk.serverwebsocket.pojo.User;
//import cn.wqk.serverwebsocket.utils.JWTUtil;
//import cn.wqk.serverwebsocket.utils.RedisUtil;
//import cn.wqk.serverwebsocket.utils.ServletUtil;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//import org.springframework.web.method.HandlerMethod;
//import org.springframework.web.servlet.HandlerMapping;
//
//import java.io.IOException;
//import java.lang.annotation.Annotation;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//
///**
// * 自定义的过滤器，用于处理请求中的Token验证逻辑
// */
//@Component
//public class TokenFilter extends OncePerRequestFilter {
//
//    @Autowired
//    private UserMapper userMapper;
//
//    private String Authorization;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        // 1. 从请求头中获取 Token
//        Authorization = request.getHeader("Authorization");
//        if (Authorization == null || Authorization.isEmpty()) {
//            Authorization = request.getHeader("Sec-WebSocket-Protocol");
//        }
//
//        // 2. 根据 shouldFilter 判断是否需要过滤
//        if (shouldFilter(request)) {
//            // 3. 验证 Token
//            User user = validateToken(Authorization, response);
//            if (user == null) return; // 验证失败已经写响应
//
//            // 4. 将用户信息存入 SecurityContext
////            LoginUser loginUser = new LoginUser(user);
////            UsernamePasswordAuthenticationToken authenticationToken =
////                    new UsernamePasswordAuthenticationToken(loginUser, null, null);
////            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
//
//            // 5. 将用户信息存入 request
//            request.setAttribute("currentUser", user);
//        }
//
//        // 6. 继续过滤器链
//        filterChain.doFilter(request, response);
//    }
//
//    /**
//     * Token 验证逻辑抽离成方法
//     * @param token 请求携带的 token
//     * @param response HttpServletResponse 用于返回错误信息
//     * @return 验证成功返回 User 对象，否则返回 null
//     */
//    private User validateToken(String token, HttpServletResponse response) throws IOException {
//        if (token == null || token.isEmpty()) {
//            ServletUtil.renderString(response, "请登录", 401);
//            return null;
//        }
//
//        int userId;
//        try {
////            userId = DecodedJWT(token);
//            userId = 1;
//        } catch (Exception e) {
//            ServletUtil.renderString(response, "请勿伪造token(解析token失败)", 401);
//            return null;
//        }
//
//        RedisUtil redisUtil = new RedisUtil();
//        String redisToken = redisUtil.get(String.valueOf(userId));
//        if (redisToken == null) {
//            ServletUtil.renderString(response, "登录已过期", 401);
//            return null;
//        }
//
//        if (!redisToken.equals(token)) {
//            ServletUtil.renderString(response, "请勿伪造token(携带的token和登陆拿到的token不一致)", 401);
//            return null;
//        }
//
//        User user = userMapper.findById(userId);
//        if (user == null) {
//            ServletUtil.renderString(response, "请勿伪造token(解析成功，但userid是伪造的)", 401);
//            return null;
//        }
//
//        if (!JWTUtil.verifyToken(token)) {
//            ServletUtil.renderString(response, "登录已过期", 401);
//            return null;
//        }
//
//        return user;
//    }
//
//    private boolean shouldFilter(HttpServletRequest request) {
//        ArrayList<String> arrayList = new ArrayList<>();
//        arrayList.add("/api/user/hello");
//        arrayList.add("/api/user/login");
//        arrayList.add("/api/file/upload");
//        String requestURI = request.getRequestURI();
//        System.out.println("requestURI = " + requestURI);
//        for (String s : arrayList ) {
//            if (s.equals(requestURI)|| requestURI.startsWith("/")) return false;
//        }
//        return true;
//    }
//
//
//
//
//    public boolean isValid(HttpServletRequest request) {
//        // 假设 HandlerMethod 作为属性存储在请求中
//        HandlerMethod handlerMethod = (HandlerMethod) request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
//        if (handlerMethod != null) {
//            Method targetMethod = handlerMethod.getMethod();
//            Annotation targetAnnotation = targetMethod.getAnnotation(LoginNotRequired.class);
//            // 如果方法被标记为 LoginNotRequired，则返回 false
//            return targetAnnotation == null;
//        }
//        // 如果 handlerMethod 不可用，继续进行过滤器链
//        return false;
//
//    }
//}
