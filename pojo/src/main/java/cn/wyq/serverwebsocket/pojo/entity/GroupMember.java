package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_member")
public class GroupMember {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String groupId;

    private String userName; // 成员账号

    /** 角色权限: 0-普通成员, 1-管理员, 2-群主 */
    private Integer role;

    private LocalDateTime joinTime;
}