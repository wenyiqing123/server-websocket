package cn.wyq.serverwebsocket.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupQueryDTO {
    // 分页参数
    private Integer page;
    private Integer pageSize;

    // 基础查询
    private Long groupId;      // 群聊ID (如果你的数据库主键是 String，请改为 String)
    private String groupName;  // 群聊名称
    private String ownerName;  // 群主名称
    private String memberName; // 群成员名称

    // 时间区间查询 (前端通常传入 "yyyy-MM-dd HH:mm:ss" 格式的字符串)
    private String startTime;  // 创建开始时间
    private String endTime;    // 创建结束时间
}