package cn.wqk.serverwebsocket.pojo;

import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Data
public class User {
    private int id;
    @NotBlank(message = "手机号不能为空")
    private String phone;
    @NotBlank(message = "用户名不能为空")
    private String userName;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String token;
}
