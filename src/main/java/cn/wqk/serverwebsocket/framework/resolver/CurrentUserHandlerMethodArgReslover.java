package cn.wqk.serverwebsocket.framework.resolver;


import cn.wqk.serverwebsocket.framework.annotation.CurrentUser;
import cn.wqk.serverwebsocket.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 用户信息解析器
 */
@Component
public class CurrentUserHandlerMethodArgReslover implements HandlerMethodArgumentResolver {

    /**
     * 判断是否支持使用@CurrentUser注解的参数
     */
    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        //如果该参数注解有@CurrentUser且参数类型是User
        return methodParameter.getParameterType().isAssignableFrom(User.class)
                && methodParameter.hasParameterAnnotation(CurrentUser.class);
    }

    /**
     * 注入参数值
     */
    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer,
                                  NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        //取得HttpServletRequest
        HttpServletRequest request = (HttpServletRequest) nativeWebRequest.getNativeRequest();
        //取出HttpServletRequest中的currentUser
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser != null) {
            return currentUser;
        }
        throw new MissingServletRequestPartException("currentUser");
    }
}