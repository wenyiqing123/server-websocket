package cn.wyq.serverwebsocket.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话表实体类
 * 对应表名：conversation
 */
@Data
@Builder
@NoArgsConstructor // 1. 生成无参构造函数（Jackson 反序列化必须）
@AllArgsConstructor // 2. 生成全参构造函数（@Builder 需要它，否则 Builder 会报错）
public class Conversation implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 对话id
     */
    private Integer id;

    /**
     * 对话名
     */
    private String name;

    /**
     * 创建时间
     * SQL类型: date
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     * SQL类型: date
     */
    private LocalDateTime updateTime;

    /**
     * 对话用户名
     */
    private String userName;
}