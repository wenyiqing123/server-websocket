package cn.wyq.serverwebsocket.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ImagePropertis：公共基础组件，为各模块提供可复用的基础能力。
 */
@Data
@Component
@ConfigurationProperties(prefix = "image")
public class ImagePropertis {
    private String mapping;
    private String location;
}