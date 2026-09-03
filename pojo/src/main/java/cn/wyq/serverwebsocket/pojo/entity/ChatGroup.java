package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ChatGroup：业务实体对象，用于映射和传递持久化数据。
 */
@Data
@TableName("chat_group")
public class ChatGroup {

    // 对应数据库的主键 id (varchar)
    @TableId(type = IdType.INPUT)
    private String id;

    // 群名称
    private String groupName;

    // 群头像图片路径 (对应数据库的 path)
    private String path;

    // 群主账号
    private String ownerName;

    // 创建时间
    private LocalDateTime createTime;
}