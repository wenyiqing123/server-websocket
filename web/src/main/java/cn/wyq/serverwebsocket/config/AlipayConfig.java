package cn.wyq.serverwebsocket.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AlipayConfig：Spring 配置类，负责注册和配置应用组件。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    // Spring Boot 会自动将 yml 中的 app-id 映射到 appId，kebab-case 转驼峰
    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String gatewayUrl;
    private String signType;
    private String charset;
    private String format;

    /**
     * 将 AlipayClient 注册为 Spring 容器中的 Bean
     * 以后在任何 Controller 或 Service 中直接 @Autowired 即可使用
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                privateKey,
                format,
                charset,
                alipayPublicKey,
                signType
        );
    }
}