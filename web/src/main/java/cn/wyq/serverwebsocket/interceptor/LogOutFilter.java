package cn.wyq.serverwebsocket.interceptor;


import cn.wyq.serverwebsocket.utils.RedisUtil;
import cn.wyq.serverwebsocket.utils.ServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;

@Component
public class LogOutFilter implements HandlerInterceptor {

    @Autowired
    private RedisUtil redisUtil;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从请求头中获取token
        String authorization = request.getHeader("Authorization");
        int userId = 0;
        // 如果Token为空，抛出未认证异常
        if (authorization == null || authorization.equals("")) {
            ServletUtil.renderString(response, "请登录", 401);
            return false;
        }
        try {
//            userId = JWTUtil.decodeToken(authorization);
            userId = 1;
        } catch (Exception e) {
            ServletUtil.renderString(response, "请勿伪造token(解析token失败)", 401);
            return false;
        }
        String token = redisUtil.get(String.valueOf(userId));
        if (token == null) {
            ServletUtil.renderString(response, "登录已过期", 401);
            return false;
        }
        if (!token.equals(authorization)) {
            ServletUtil.renderString(response, "登录已过期", 401);
            return false;
        }
        redisUtil.delete(String.valueOf(userId));
        return true;
    }

    private boolean shouldFilter(HttpServletRequest request) {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("/user/logout");
        String requestURI = request.getRequestURI();
        // 根据实际需求检查请求路径是否需要进行拦截
        for (String s : arrayList) {
            if (s.equals(requestURI)) {
                return false;
            }
        }
        return true;
    }


}
