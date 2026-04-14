package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.GroupCreateDTO;

import java.util.List;


public interface GroupService {

    // 在 GroupService 接口中新增：

    Result getGroupMembers(String groupId);

    /**
     * 根据群聊ID获取所有群成员的账号名
     */
    List<String> getGroupMemberNames(String groupId);

    /**
     * 创建群聊
     */
    Result createGroup(GroupCreateDTO dto);

    /**
     * 获取我加入的群聊列表
     */
    Result getMyGroups(String userName);


}