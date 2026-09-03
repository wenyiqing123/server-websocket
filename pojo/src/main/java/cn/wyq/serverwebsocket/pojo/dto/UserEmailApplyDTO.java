package cn.wyq.serverwebsocket.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserEmailApplyDTO：数据传输对象，用于承载接口请求参数。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEmailApplyDTO {
    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 绑定的邮箱
     */
    private String email;

    /**
     * 邮箱验证码
     */
    private String code;

    /**
     * 🌟 新增：申请的角色类型
     * 0 或 null：代表注册普通用户
     * 1：代表申请成为管理员
     */
    private Integer applyRole;
}
