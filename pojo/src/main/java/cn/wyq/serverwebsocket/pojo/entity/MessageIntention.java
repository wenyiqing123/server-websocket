package cn.wyq.serverwebsocket.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MessageIntention：业务实体对象，用于映射和传递持久化数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageIntention {
    private int id;
    private int userId;
    private String tagName;
    private String originalPrompt;
    private LocalDateTime createTime;
}
