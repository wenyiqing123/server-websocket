//package cn.wqk.serverwebsocket.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.filter.CorsFilter;
//
//@Configuration
//public class CorsConfig {
//
//    @Bean
//    public CorsFilter corsFilter() {
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        CorsConfiguration config = new CorsConfiguration();
//
//        // 允许来自 http://localhost:8080 的跨域请求
//        config.addAllowedOrigin("https://web-websocket-nt8w3lckm-linzeshuis-projects.vercel.app");
//        config.addAllowedOrigin("https://localhost:8080");
//        config.addAllowedHeader("*");
//        config.addAllowedMethod("OPTIONS");
//        config.addAllowedMethod("GET");
//        config.addAllowedMethod("POST");
//        config.addAllowedMethod("PUT");
//        config.addAllowedMethod("DELETE");
//
//        source.registerCorsConfiguration("/user/**", config); // 针对特定路径生效，可以根据实际情况修改
//
//        return new CorsFilter(source);
//    }
//}
