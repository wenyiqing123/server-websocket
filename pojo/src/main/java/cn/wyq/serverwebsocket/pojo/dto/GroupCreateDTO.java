package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupCreateDTO {
    private String groupName;
    private String groupAvatar;
    private String ownerName;         // 当前操作建群的人
    private List<String> memberNames; // 初始邀请的好友账号列表
}