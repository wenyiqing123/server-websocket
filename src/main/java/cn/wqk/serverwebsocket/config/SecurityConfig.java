package cn.wqk.serverwebsocket.config;


import cn.wqk.serverwebsocket.exception.AuthenticationEntryPointException;
import cn.wqk.serverwebsocket.filter.TokenFilter;
import cn.wqk.serverwebsocket.impl.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Security配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig implements WebMvcConfigurer {

    @Autowired

    private UserDetailsService userDetailsService;
    @Autowired
    private TokenFilter tokenFilter;

    /**
     * 创建BCryptPasswordEncoder实例并注入容器
     *
     * @return BCryptPasswordEncoder实例
     */
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPointException authenticationEntryPointException() {
        return new AuthenticationEntryPointException();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsServiceImpl();
    }

    /**
     * AuthenticationManager：配置身份验证管理器
     * DaoAuthenticationProvider：关联provider和passwordEncoder于AuthenticationManager中
     *
     * @param passwordEncoder
     * @return
     * @throws Exception
     */
    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService); // 修复此处

        //关联密码编码器
        provider.setPasswordEncoder(passwordEncoder);

        //将provider放置于AuthenticationManager
        ProviderManager providerManager = new ProviderManager(provider);
        return providerManager;
    }

    /**
     * 配置安全过滤器链，限定部分请求都需要身份验证
     *
     * @param http HttpSecurity对象
     * @return SecurityFilterChain实例
     * @throws Exception 异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 禁用 CSRF（Cross-Site Request Forgery）防护，因为我们使用了无状态的认证机制(token)
        http.addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class).csrf(csrf -> csrf.disable())
                // 配置异常处理，未授权时返回自定义的入口点
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPointException()))
                // 设置会话管理策略为无状态，即不使用 HttpSession 进行会话跟踪
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 允许访问 "/api/auth/**" 路径的请求，不进行身份验证
                        .requestMatchers("/user/login", "/user/register", "/user/logout", "/websocket/**")
                        //随意访问
                        .permitAll()
                        // 对于其他任何请求，要求进行身份验证
                        .anyRequest().authenticated());
        return http.build();
    }


    /**
     * 配置跨域
     *
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 配置全局CORS策略，允许所有的跨域请求
        registry.addMapping("/**")
                // 允许的来源，明确指定允许的域，根据实际情况修改
                .allowedOrigins("http://localhost:8080", "ws://localhost:8080")
//                .allowedOrigins("*")
                // 是否允许发送Cookie信息，此处设置为允许
                .allowCredentials(true)
                // 允许的请求方法，使用全大写，根据实际情况修改
                .allowedMethods("POST", "DELETE", "PUT", "PATCH", "GET")
                // 允许的请求头，此处设置为允许所有请求头，可以根据实际情况修改
                .allowedHeaders("*")
                // 预检请求的有效期，单位为秒，此处设置为1小时，可以根据实际情况修改
                .maxAge(3600);
    }

//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        // 配置全局CORS策略，允许所有的跨域请求
//        // 映射的路径，此处为所有路径
//        registry.addMapping("/**")
//                // 允许的来源，此处设置为允许所有来源，可以根据实际情况修改
//                .allowedOriginPatterns("*")
//                // 允许的来源，明确指定允许的域，根据实际情况修改
//                .allowedOrigins("http://localhost:8079")
//                // 是否允许发送Cookie信息，此处设置为允许
//                .allowCredentials(true)
//                // 允许的请求方法，可以根据实际情况修改
//                .allowedMethods("Post", "Delete", "Put", "Patch", "Get")
//                // 允许的请求头，此处设置为允许所有请求头，可以根据实际情况修改
//                .allowedHeaders("*")
//                // 预检请求的有效期，单位为秒，此处设置为1小时，可以根据实际情况修改
//                .maxAge(3600);
//    }
}
