package cn.wyq.serverwebsocket.config;

import cn.wyq.serverwebsocket.utils.JWTUtil;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.Map;


/**
 * JWT 安全配置。
 *
 * <p>签名密钥优先从 {@code JWT_SECRET} 环境变量读取；未配置时使用
 * {@code application.yaml} 中的固定默认值 {@code jwt_secret_for_server_websocket}，
 * 确保本地开发可以直接启动。生产部署仍建议通过环境变量覆盖默认值。</p>
 */
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class JwtSecurityConfig {

    private String secret;

    /** 运行环境标识，用于保留生产配置校验。 */
    @Value("${app.environment:development}")
    private String appEnvironment;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SysConfigService sysConfigService;

    /**
     * 绑定外部 JWT 签名密钥。
     *
     * @param secret 外部配置值
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * 在应用接受请求前初始化公共 JWT 工具。
     */
    @PostConstruct
    public void initializeJwtSecret() {
        if (!StringUtils.hasText(secret)) {
            if (isStrictEnvironment()) {
                throw new IllegalStateException("JWT_SECRET must be configured in production-like environments");
            }
            throw new IllegalStateException("JWT signing secret is missing; configure JWT_SECRET or application.yaml default");
        }
        JWTUtil.configureSecret(secret.trim());
        configureTokenExpiryFromDatabase();
    }

    /** 启动时预加载平台令牌有效期；后续每次签发令牌仍会再次读取以支持热更新。 */
    private void configureTokenExpiryFromDatabase() {
        try {
            Map<String, String> config = sysConfigService == null
                    ? Collections.emptyMap() : sysConfigService.getConfigMap();
            long accessMinutes = SystemConfigUtils.longValue(
                    config, "jwt_access_token_minutes", 120L, 5L, 10_080L);
            long refreshDays = SystemConfigUtils.longValue(
                    config, "jwt_refresh_token_days", 30L, 1L, 365L);
            JWTUtil.configureTokenExpiry(accessMinutes, refreshDays);
        } catch (RuntimeException exception) {
            // 数据库配置暂时不可用时仍允许应用启动，使用稳定的默认期限。
            JWTUtil.configureTokenExpiry(120L, 30L);
        }
    }

    private boolean isStrictEnvironment() {
        return "prod".equalsIgnoreCase(appEnvironment)
                || "production".equalsIgnoreCase(appEnvironment)
                || "stage".equalsIgnoreCase(appEnvironment)
                || "staging".equalsIgnoreCase(appEnvironment);
    }

}
