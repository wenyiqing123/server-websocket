package cn.wyq.serverwebsocket.config;

import cn.wyq.serverwebsocket.common.ImagePropertis;
import cn.wyq.serverwebsocket.common.JacksonObjectMapper;
import cn.wyq.serverwebsocket.interceptor.JWTInterceptor;
import cn.wyq.serverwebsocket.interceptor.LogOutFilter;
import cn.wyq.serverwebsocket.resolver.CurrentUserHandlerMethodArgReslover;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * 终极合并版：全局 Web 配置类
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ImagePropertis imageProperties;

    @Autowired
    private JWTInterceptor jwtInterceptor;

    // JWT 拦截器放行的白名单
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

    // ================== 1. Bean 注册区域 ==================

    @Bean
    public LogOutFilter logOutFilter() {
        return new LogOutFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ================== 2. 静态资源映射 ==================

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 自定义图片映射
        registry.addResourceHandler(imageProperties.getMapping() + "**")
                .addResourceLocations("file:" + imageProperties.getLocation() + "/");

        // 普通静态资源映射与 favicon
        registry.addResourceHandler("/static/**", "/favicon.ico")
                .addResourceLocations("classpath:/static/");

        // Knife4j / Swagger 接口文档映射
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    // ================== 3. 拦截器配置 ==================

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 JWT 全局登录拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_PATHS);

        // 注册登出过滤器（针对特定路径）
        registry.addInterceptor(logOutFilter())
                .addPathPatterns("/user/logout");
    }

    // ================== 4. 参数解析器配置 ==================

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 注册自定义参数解析器（如 @CurrentUser）
        argumentResolvers.add(new CurrentUserHandlerMethodArgReslover());
    }

    // ================== 5. 跨域与消息转换器 ==================

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .maxAge(3600);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器：应用 JacksonObjectMapper 统一日期格式");
        MappingJackson2HttpMessageConverter customConverter = new MappingJackson2HttpMessageConverter();
        customConverter.setObjectMapper(new JacksonObjectMapper());

        // 🚨 核心修复：不能使用 converters.add(0, customConverter);
        // 遍历现有的转换器，找到 Spring 默认的 JSON 转换器并将其平替。
        // 这样不仅应用了我们的日期格式，还保住了 ByteArray 转换器的高优先级，防止 Swagger 文档变成 Base64
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof MappingJackson2HttpMessageConverter) {
                converters.set(i, customConverter);
                return;
            }
        }

        // 兜底逻辑：如果没找到，就追加到末尾
        converters.add(customConverter);
    }
}