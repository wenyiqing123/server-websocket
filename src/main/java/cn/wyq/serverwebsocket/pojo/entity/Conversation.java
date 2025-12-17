package cn.wyq.serverwebsocket.pojo.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 对话表实体类
 * 对应表名：conversation
 */
@Data
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
    private Date createTime;

    /**
     * 更新时间
     * SQL类型: date
     */
    private Date updateTime;

    /**
     * 对话用户名
     */
    private String userName;
}