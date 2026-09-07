package cn.wyq.serverwebsocket.config;

import cn.wyq.serverwebsocket.util.JWTUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 配置与握手鉴权。
 *
 * <p>浏览器无法在原生 WebSocket API 中直接设置 Authorization 请求头，因此支持
 * {@code ?token=} 查询参数和既有的 {@code Sec-WebSocket-Protocol} 传递方式。
 * 任何一种方式都必须通过 JWT 验签，并且 Token 内用户名必须与端点路径完全一致。</p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig extends ServerEndpointConfig.Configurator {

    /** 已认证用户名在 WebSocket 会话属性中的键。 */
    public static final String AUTHENTICATED_USERNAME = "websocket.authenticatedUsername";

    /** 已认证用户 ID 在 WebSocket 会话属性中的键。 */
    public static final String AUTHENTICATED_USER_ID = "websocket.authenticatedUserId";

    private static final String TOKEN_PARAMETER = "token";
    private static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

    /**
     * 注册 ServerEndpointExporter，使 Spring Boot 扫描 {@code @ServerEndpoint}。
     *
     * @return WebSocket endpoint 导出器
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * 在升级连接前完成 WebSocket 身份认证。
     *
     * @param sec WebSocket 端点配置
     * @param request 握手请求
     * @param response 握手响应
     */
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        String requestedUsername = extractPathUsername(request);
        HandshakeToken handshakeToken = extractToken(request);
        if (handshakeToken == null || !JWTUtil.verifyAccessToken(handshakeToken.token())) {
            throw rejectedConnection();
        }

        try {
            String tokenUsername = JWTUtil.getUsername(handshakeToken.token());
            int userId = JWTUtil.getUserId(handshakeToken.token());
            if (tokenUsername == null || tokenUsername.isBlank() || userId <= 0
                    || !tokenUsername.equals(requestedUsername)) {
                throw rejectedConnection();
            }

            sec.getUserProperties().put(AUTHENTICATED_USERNAME, tokenUsername);
            sec.getUserProperties().put(AUTHENTICATED_USER_ID, userId);

            HttpSession httpSession = (HttpSession) request.getHttpSession();
            if (httpSession != null) {
                sec.getUserProperties().put(HttpSession.class.getName(), httpSession);
            }

            // 仅在客户端确实用子协议传 Token 时回写，避免 query-token 客户端握手失败。
            if (handshakeToken.subProtocol() != null) {
                response.getHeaders().put(SEC_WEBSOCKET_PROTOCOL, List.of(handshakeToken.subProtocol()));
            }
        } catch (SecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw rejectedConnection();
        }
    }

    /**
     * 从端点路径提取客户端声明的用户名；该值必须在后续与 Token Claim 精确匹配。
     */
    private String extractPathUsername(HandshakeRequest request) {
        URI requestUri = request.getRequestURI();
        String path = requestUri == null ? null : requestUri.getPath();
        if (path == null || path.isBlank()) {
            throw rejectedConnection();
        }

        int separatorIndex = path.lastIndexOf('/');
        String username = separatorIndex < 0 ? path : path.substring(separatorIndex + 1);
        if (username.isBlank()) {
            throw rejectedConnection();
        }
        return username;
    }

    /**
     * 先读取 query token；没有时兼容原有的 Sec-WebSocket-Protocol Token。
     */
    private HandshakeToken extractToken(HandshakeRequest request) {
        Map<String, List<String>> parameters = request.getParameterMap();
        if (parameters != null) {
            List<String> tokens = parameters.get(TOKEN_PARAMETER);
            if (tokens != null) {
                for (String token : tokens) {
                    String normalizedToken = normalizeToken(token);
                    if (normalizedToken != null) {
                        return new HandshakeToken(normalizedToken, null);
                    }
                }
            }
        }

        Map<String, List<String>> headers = request.getHeaders();
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if (!SEC_WEBSOCKET_PROTOCOL.equalsIgnoreCase(header.getKey()) || header.getValue() == null) {
                continue;
            }
            for (String headerValue : header.getValue()) {
                if (headerValue == null) {
                    continue;
                }
                for (String protocol : headerValue.split(",")) {
                    String candidate = protocol.trim();
                    String normalizedToken = normalizeToken(candidate);
                    // 子协议必须原样回写；Bearer 格式不符合浏览器子协议语法，故只接受原始 JWT。
                    if (normalizedToken != null && candidate.equals(normalizedToken)) {
                        return new HandshakeToken(normalizedToken, candidate);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 提取原始 JWT；query 参数额外兼容 Bearer 前缀，便于客户端复用 access token。
     */
    private String normalizeToken(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.trim();
        if (normalized.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            normalized = normalized.substring("Bearer ".length()).trim();
        }
        return normalized.isBlank() ? null : normalized;
    }

    private SecurityException rejectedConnection() {
        return new SecurityException("WebSocket authentication failed");
    }

    /**
     * 握手阶段保留 Token 与是否来自子协议的最小必要信息。
     */
    private record HandshakeToken(String token, String subProtocol) {
    }
}
