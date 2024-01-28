package cn.wqk.serverwebsocket.framework.exception;


import cn.wqk.serverwebsocket.framework.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

@Data
public class AuthenticationEntryPointException extends RuntimeException implements AuthenticationEntryPoint {

    private int code;

    public AuthenticationEntryPointException(String message, int code) {
        super(message);
        this.code = code;
    }

    public AuthenticationEntryPointException(String message) {
        super(message);
    }

    public AuthenticationEntryPointException() {

    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        // 使用 Jackson 库将 Result 对象转换为 JSON 字符串
        Result error = Result.error(401, "用户信息认证失败");
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResult = objectMapper.writeValueAsString(error);
        response.getWriter().write(jsonResult);
    }
}
