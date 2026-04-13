package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.GroupCreateDTO;


public interface GroupService {
    /**
     * 创建群聊
     */
    Result createGroup(GroupCreateDTO dto);

    /**
     * 获取我加入的群聊列表
     */
    Result getMyGroups(String userName);
}