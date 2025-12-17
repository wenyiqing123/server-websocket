package cn.wyq.serverwebsocket.framework.bean;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "image")
public class ImagePropertis {
    private String mapping;
    private String location;
}
