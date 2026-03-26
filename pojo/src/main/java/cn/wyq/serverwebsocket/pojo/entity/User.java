package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.tool.annotation.ToolParam;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("user") // 💡 假设你的数据库表名叫 user，请改成你真实的表名
public class User {

    @TableId(type = IdType.AUTO) // 💡 告诉 MP 这是主键，且是自增的
    @ToolParam(required = false, description = "用户编号")
    private Integer id; // ⚠️ 改为 Integer

    @ToolParam(required = false, description = "用户名")
    private String userName;

    @ToolParam(required = false, description = "用户密码")
    private String password;

    @ToolParam(required = false, description = "用户头像路径")
    private String path;

    @ToolParam(required = false, description = "用户角色=0：超级管理员，1：管理员，2：用户")
    private Integer role; // ⚠️ 改为 Integer
}