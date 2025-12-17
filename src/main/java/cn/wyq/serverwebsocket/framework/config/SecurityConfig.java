//package cn.wqk.serverwebsocket.framework.config;
//
//import cn.wqk.serverwebsocket.framework.exception.AuthenticationEntryPointException;
//import cn.wqk.serverwebsocket.framework.filter.TokenFilter;
//import cn.wqk.serverwebsocket.framework.security.impl.UserDetailsServiceImpl;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.ProviderManager;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
///**
// * Spring Security 配置类
// *
// * 功能：
// * 1. 配置密码编码器
// * 2. 配置 AuthenticationManager
// * 3. 配置安全过滤器链（SecurityFilterChain）
// * 4. 配置跨域（CORS）
// */
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//public class SecurityConfig implements WebMvcConfigurer {
//
//    @Autowired
//    private UserDetailsService userDetailsService;
//
//    @Autowired
//    private TokenFilter tokenFilter;
//
//    @Bean
//    public WebSecurityCustomizer securityCustomizer() {
//        return (web) -> web.ignoring().requestMatchers("/static/**", "/head/**");
//    }
//
//
//
//    /**
//     * 创建 BCryptPasswordEncoder 实例
//     * 用于密码加密和验证
//     */
//    @Bean
//    public static PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    /**
//     * 自定义未授权处理器 Bean
//     * 当用户未认证或 token 无效时，会调用该处理器
//     */
//    @Bean
//    public AuthenticationEntryPointException authenticationEntryPointException() {
//        return new AuthenticationEntryPointException();
//    }
//
//    /**
//     * UserDetailsService Bean
//     * 用于加载用户信息
//     */
//    @Bean
//    public UserDetailsService userDetailsService() {
//        return new UserDetailsServiceImpl();
//    }
//
//    /**
//     * 配置 AuthenticationManager
//     * 关联 DaoAuthenticationProvider 和密码编码器
//     */
//    @Bean
//    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) throws Exception {
//        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
//        provider.setUserDetailsService(userDetailsService); // 绑定 UserDetailsService
//        provider.setPasswordEncoder(passwordEncoder);       // 绑定密码编码器
//        return new ProviderManager(provider);               // 注入 ProviderManager
//    }
//
//    /**
//     * 配置 Spring Security 过滤器链
//     * - 添加自定义 TokenFilter
//     * - 禁用 CSRF
//     * - 配置会话为无状态（token 认证）
//     * - 配置请求授权规则
//     */
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                // 1. 添加自定义 TokenFilter 到 UsernamePasswordAuthenticationFilter 之前
//                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
//
//                // 2. 禁用 CSRF，因为使用的是 token 认证，不依赖 session
//                .csrf(csrf -> csrf.disable())
//
//                // 3. 配置异常处理，未授权返回自定义入口点
//                .exceptionHandling(exception ->
//                        exception.authenticationEntryPoint(authenticationEntryPointException())
//                )
//
//                // 4. 会话管理策略：无状态，不创建 HttpSession
//                .sessionManagement(session ->
//                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                )
//
//                // 5. 请求授权规则
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/file/**", "/static/**").permitAll()
//                        // 允许访问的路径，无需认证
//                        .requestMatchers(
//                                "/file/**",
//                                "/**",
//                                "/user/login", "/user/register", "/user/logout",
//                                "/websocket/**", "/user/hello", "/static/**",
//                                "/user/users", "/test/remoteProcedureCall"
//                        ).permitAll()
//                        // 其他请求都需要认证
//                        .anyRequest().authenticated()
//                );
////        http.authorizeHttpRequests(auth -> auth
////                .requestMatchers("/file/**").permitAll()
////                .anyRequest().authenticated()
////        );
//
//
//        return http.build();
//    }
//
//    /**
//     * 配置全局 CORS 策略
//     * - 允许指定域名跨域访问
//     * - 允许 Cookie
//     * - 支持多种请求方法
//     */
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**") // 对所有路径生效
//                .allowedOrigins(
//                        "http://localhost:8080",
//                        "localhost",
//                        "ws://localhost:8080",
//                        "http://47.99.45.73:8001"
//                )
//                .allowCredentials(true)                  // 允许携带 Cookie
//                .allowedMethods("POST", "DELETE", "PUT", "PATCH", "GET") // 支持的请求方法
//                .allowedHeaders("*")                     // 允许所有请求头
//                .maxAge(3600);                           // 预检请求缓存时间 1 小时
//    }
//
//}
