package cn.wyq.serverwebsocket.framework.config;

import cn.wyq.serverwebsocket.framework.bean.ImagePropertis;
import cn.wyq.serverwebsocket.framework.interceptor.JWTFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * 全局 Web 配置类
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ImagePropertis imageProperties;

    @Autowired
    private JWTFilter jwtFilter;

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/user/path",
            "/user/login",
            "/user/register",
            "/doc.html",
            "/webjars/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/favicon.ico",
            "/error",
            "/static/**",
            "/file/upload"
    );

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ❌ 删除或注释掉下面这两行（因为 AutoConfig 会自动处理，写了反而报错）
        // registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        // registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");

        // ✅ 保留你自定义的图片映射
        registry.addResourceHandler(imageProperties.getMapping() + "**")
                .addResourceLocations("file:" + imageProperties.getLocation() + "/");

        // ✅ 保留普通静态资源映射
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        //静态资源映射，不然访问不到接口文档doc.html，防止springMVC认为请求的是一个接口，而不是静态资源
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 配置跨域请求，则类上不需要添加 @CrossOrigin 注解
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtFilter)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }

//    @Override
//    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
//        log.info("开始扩展消息转换器");
//        //创建消息转换器对象
//        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
//        //设置对象转换器，底层使用Jackson将Java对象转为json
//        converter.setObjectMapper(new JacksonObjectMapper());
//        //将上面的消息转换器对象追加到mvc框架的转换器集合中
//        converters.add(0,converter);
//    }
}