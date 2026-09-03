package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

/**
 * ActiveUsersAndMessageCount：视图对象，用于组织接口返回和展示数据。
 */
@Data
public class ActiveUsersAndMessageCount {
    private String userName;
    private int messageCount;
    private String path;
}
