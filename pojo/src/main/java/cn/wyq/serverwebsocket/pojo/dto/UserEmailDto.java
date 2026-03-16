package cn.wyq.serverwebsocket.pojo.dto;

import lombok.*;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class UserEmailDto {
    private int id;
    @NotBlank(message = "用户名不能为空")
    private String userName;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String path;
    private int role = 2;
    private String email;
    private String code;
}
