package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * GroupCreateDTO：数据传输对象，用于承载接口请求参数。
 */
@Data
public class GroupCreateDTO {
    private String groupName;
    private String groupAvatar;
    private String ownerName;         // 当前操作建群的人
    private List<String> memberNames; // 初始邀请的好友账号列表
}