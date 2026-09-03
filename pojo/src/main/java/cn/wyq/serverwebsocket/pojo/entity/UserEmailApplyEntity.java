package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserEmailApplyEntity：业务实体对象，用于映射和传递持久化数据。
 */
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class UserEmailApplyEntity {
    private int id;
    private String userName;
    private String password;
    private String path;
    private Integer role;
    /**
     * 绑定邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 账号状态：0-待审批，1-正常，2-拒绝/封禁
     */
    @TableField("status")
    private Integer status;
}
