package cn.wqk.serverwebsocket.pojo;

import lombok.Data;

@Data
public class User {
    private String id;
//    @NotBlank(message = "用户名不能为空")
    private String userName;
//    @NotBlank(message = "密码不能为空")
    private String password;
}
