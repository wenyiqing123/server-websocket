package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.GroupCreateDTO;

import java.util.List;


public interface GroupService {


    /**
     * 获取群成员详情列表（包含头像和角色）
     */
    Result getGroupMembersDetail(String groupId);

    /**
     * 修改群信息（名称、头像等）
     */
    Result updateGroupInfo(String groupId, String groupName, String groupAvatar, String operatorName);

    /**
     * 踢出群成员（仅群主/管理员可用）
     */
    Result kickGroupMember(String groupId, String targetUserName, String operatorName);

    /**
     * 主动退出群聊
     */
    Result quitGroup(String groupId, String userName);

    /**
     * 解散群聊（仅群主可用）
     */
    Result dismissGroup(String groupId, String operatorName);
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