package cn.wyq.serverwebsocket.framework.config;

import cn.wyq.serverwebsocket.framework.bean.ImagePropertis;
import cn.wyq.serverwebsocket.framework.interceptor.LogOutFilter;
import cn.wyq.serverwebsocket.framework.resolver.CurrentUserHandlerMethodArgReslover;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebMvc
public class interceptorConfig implements WebMvcConfigurer {

    private List<String> alist = new ArrayList<String>();
    @Autowired
    ImagePropertis image;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(image.getMapping() + "**")
                .addResourceLocations("file:" + image.getLocation() + "/");

        //配置 knife4j 的静态资源请求映射地址
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }


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

    /**
     * //     * 创建 BCryptPasswordEncoder 实例
     * //     * 用于密码加密和验证
     * //
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
