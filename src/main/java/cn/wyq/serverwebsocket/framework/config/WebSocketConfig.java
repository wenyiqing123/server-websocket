package cn.wyq.serverwebsocket.framework.config;

import cn.wyq.serverwebsocket.framework.common.ApplicationHelper;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 配置类
 * - 开启 Spring 对 WebSocket 的支持
 * - 通过 ServerEndpointConfig.Configurator 在握手阶段处理 token
 */
@Configuration
@EnableWebSocket

public class WebSocketConfig extends ServerEndpointConfig.Configurator {

    @Autowired
    private UserMapper userMapper;


    /**
     * 注册 ServerEndpointExporter
     * Spring Boot 中必须声明此 Bean，才能扫描 @ServerEndpoint 注解
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * WebSocket 握手阶段处理方法
     *
     * @param sec      WebSocket 配置对象
     * @param request  握手请求对象
     * @param response 握手响应对象
     */
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        UserService userService = ApplicationHelper.getBean(UserService.class);
        UserMapper userMapper = ApplicationHelper.getBean(UserMapper.class);
        // 1. 获取 HttpSession
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        // 将 HttpSession 保存到 WebSocket 的 userProperties 中
        sec.getUserProperties().put(HttpSession.class.getName(), httpSession);
        // 2. 获取前端传来的 token
        // 从 Sec-WebSocket-Protocol 头获取 token
        Map<String, List<String>> headers = request.getHeaders();
        List<String> protocolList = headers.get("sec-websocket-protocol");
        String token = null;

        if (protocolList != null && !protocolList.isEmpty()) {
            token = protocolList.get(0); // 取第一个协议值作为 token
            // 将 token 存入 userProperties，后续 WebSocket session 中可获取
            sec.getUserProperties().put("token", token);
        }
        // 3. 回写 Sec-WebSocket-Protocol
        // 如果 token 不为空，需要回写给浏览器，否则浏览器会拒绝握手
        if (token != null) {
            response.getHeaders().put("Sec-WebSocket-Protocol", List.of(token));
//            // TODO: 这里可以加 token 校验逻辑，例如 JWT 或 Redis 验证
//            if (userService.validateToken(token) != null) {
//                // 校验通过后，可以存入 userProperties
//                int userId = JWTUtil.decodeToken(token);
//                User user = userMapper.findById(userId);
//                sec.getUserProperties().put("currentUser", user);
//                // 回写 Sec-WebSocket-Protocol
//                response.getHeaders().put("Sec-WebSocket-Protocol", List.of(token));
//            }
//        } else {
//            throw new RuntimeException("Token 无效，拒绝握手");
        }
    }
}
