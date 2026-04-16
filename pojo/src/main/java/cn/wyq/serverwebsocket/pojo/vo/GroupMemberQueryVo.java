package cn.wyq.serverwebsocket.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupMemberQueryVo {
    //群成员id（不是用户id）
    private Integer id;
    //所属群组id
    private String groupId;
    //加入群组时间
    private LocalDateTime joinTime;
    //用户名
    private String userName;
    //角色
    private     int role;
    //头像地址
    private String path;
}
