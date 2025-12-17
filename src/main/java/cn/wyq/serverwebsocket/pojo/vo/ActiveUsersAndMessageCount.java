package cn.wyq.serverwebsocket.pojo.vo;

import lombok.Data;

@Data
public class ActiveUsersAndMessageCount {
    private String userName;
    private int messageCount;
    private String path;
}
