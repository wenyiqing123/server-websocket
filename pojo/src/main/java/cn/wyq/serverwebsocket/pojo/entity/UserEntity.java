package cn.wyq.serverwebsocket.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.tool.annotation.ToolParam;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEntity {
    @ToolParam(required = false,description = "用户编号")
    private int id;
    @ToolParam(required = false,description = "用户密码")
    private String password;
    @ToolParam(required = false,description = "用户名")
    private String userName;
    @ToolParam(required = false,description = "用户头像路径")
    private String path;
    @ToolParam(required = false,description = "用户角色=0：超级管理员，1：管理员，2：用户")
    private int role;
}
