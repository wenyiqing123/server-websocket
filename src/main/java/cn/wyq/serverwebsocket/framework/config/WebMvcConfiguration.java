//package cn.wyq.serverwebsocket.framework.config;
//
//import cn.wyq.serverwebsocket.framework.bean.ImagePropertis;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
//
///**
// * @author wyq
// * 2024/12/29
// */
//@Configuration
//public class WebMvcConfiguration extends WebMvcConfigurationSupport {
//    @Autowired
//    ImagePropertis image;
//
//    /**
//     * 设置静态资源映射
//     *
//     * @param registry
//     */
//    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler(image.getMapping() + "**")
//                .addResourceLocations("file:" + image.getLocation() + "/");
//
//    }
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("http://localhost:8080")
//                .allowCredentials(true)
//                .allowedMethods("*")
//                .maxAge(3600);
//    }
//
//}
