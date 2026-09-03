package cn.wyq.serverwebsocket.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Chat：业务实体对象，用于映射和传递持久化数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Chat {

    private String id;
    private String userId;
    private String title;
    private LocalDate createdAt; // 对应数据库的 created_at
}
