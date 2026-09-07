package cn.wyq.serverwebsocket.resolver;


import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * CurrentUserHandlerMethodArgReslover：参数解析组件，负责将请求信息转换为方法参数。
 */
@Component
public class CurrentUserHandlerMethodArgReslover implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType().isAssignableFrom(UserEntity.class)
                && methodParameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer,
                                  NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {

        HttpServletRequest request = (HttpServletRequest) nativeWebRequest.getNativeRequest();

        // 1. 注意：这里要对应你在 JWTInterceptor 中 setAttribute 的名字
        // 你的 JWTInterceptor 里写的是 request.setAttribute("userId", userId);
        Object userId = request.getAttribute("userId");
        Object username = request.getAttribute("username");
        // 2. 逻辑判断
        if (userId != null) {
            // 方案 A：如果你只想快速获取 ID 和用户名，直接 new 一个对象返回

            UserEntity userEntity = new UserEntity();
            userEntity.setId(Integer.parseInt(userId.toString()));
            userEntity.setUserName((String) username);
            return userEntity;
            // 方案 B：如果你需要完整的用户信息（比如头像、角色），则调用 Service 查库
//            return userService.findById(Integer.parseInt(userId.toString()));
        }

        // 标记了 @CurrentUser 的接口都需要登录，避免控制器解引用 null 后变成 500。
        throw new ServiceException("请先登录", 401);
    }
}
