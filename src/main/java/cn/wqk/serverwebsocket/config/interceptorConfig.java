package cn.wqk.serverwebsocket.config;

import cn.wqk.serverwebsocket.interceptor.LogOutFilter;
import cn.wqk.serverwebsocket.resolver.CurrentUserHandlerMethodArgReslover;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class interceptorConfig implements WebMvcConfigurer {

    private List<String> alist = new ArrayList<String>();

    /**
     * 初始化自定义拦截器Bean
     *
     * @return LogOutFilter
     */
    @Bean
    public LogOutFilter init() {
        return new LogOutFilter();
    }

    /**
     * 添加拦截器配置
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        alist.add("/user/logout");
        //添加拦截器
        registry.addInterceptor(init())
                //设置拦截路径
                .addPathPatterns(alist);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        //注册@CurrentUser注解的实现类
        argumentResolvers.add(new CurrentUserHandlerMethodArgReslover());
    }
}
