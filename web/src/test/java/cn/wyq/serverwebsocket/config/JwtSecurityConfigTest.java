package cn.wyq.serverwebsocket.config;

import cn.wyq.serverwebsocket.utils.JWTUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtSecurityConfigTest {

    @Test
    void configuredSecretIsUsableInDevelopmentEnvironment() throws Exception {
        JwtSecurityConfig config = new JwtSecurityConfig();
        setEnvironment(config, "development");
        config.setSecret("jwt_secret_for_server_websocket");

        config.initializeJwtSecret();

        assertTrue(JWTUtil.verifyAccessToken(JWTUtil.generateToken(1, "development-user")));
    }

    @Test
    void productionEnvironmentRequiresConfiguredSecret() throws Exception {
        JwtSecurityConfig config = new JwtSecurityConfig();
        setEnvironment(config, "production");

        assertThrows(IllegalStateException.class, config::initializeJwtSecret);
    }

    private void setEnvironment(JwtSecurityConfig config, String environment) throws Exception {
        Field field = JwtSecurityConfig.class.getDeclaredField("appEnvironment");
        field.setAccessible(true);
        field.set(config, environment);
    }
}
