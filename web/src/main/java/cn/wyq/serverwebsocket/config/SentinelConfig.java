package cn.wyq.serverwebsocket.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentinelConfig {

    // 1. 注入切面（没有它，@SentinelResource 绝对不生效）
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    // 从 application.yml 读取配置
    @Value("${spring.application.name:server-websocket}")
    private String projectName;

    @Value("${spring.cloud.sentinel.transport.dashboard:localhost:8090}")
    private String dashboard;

    @Value("${spring.cloud.sentinel.transport.port:8719}")
    private String apiPort;

    // 2. 在 Spring 启动时，把 YAML 的配置塞给 Sentinel 的底层系统变量
    @PostConstruct
    public void initSentinel() {
        System.setProperty("project.name", projectName);
        System.setProperty("csp.sentinel.dashboard.server", dashboard);
        System.setProperty("csp.sentinel.api.port", apiPort);
    }
}