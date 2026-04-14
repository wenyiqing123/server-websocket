package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.mapper.ChatGroupMapper;
import cn.wyq.serverwebsocket.mapper.GroupMemberMapper;
import cn.wyq.serverwebsocket.pojo.dto.GroupCreateDTO;
import cn.wyq.serverwebsocket.pojo.entity.ChatGroup;
import cn.wyq.serverwebsocket.pojo.entity.GroupMember;
import cn.wyq.serverwebsocket.service.GroupService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private ChatGroupMapper chatGroupMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;


    @Override
    public List<String> getGroupMemberNames(String groupId) {
        QueryWrapper<GroupMember> query = new QueryWrapper<>();
        query.eq("group_id", groupId);
        List<GroupMember> members = groupMemberMapper.selectList(query);

        // 提取所有成员的 userName 并返回一个 String 列表
        return members.stream()
                .map(GroupMember::getUserName)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createGroup(GroupCreateDTO dto) {
        // 🌟 1. 强制校验：必须设置群头像
        if (dto.getGroupAvatar() == null || dto.getGroupAvatar().trim().isEmpty()) {
            return Result.error("建群失败：必须上传或设置群头像！");
        }

        // 🌟 2. 强制校验：勾选成员必须 >= 2 人 (加上群主共3人)
        if (dto.getMemberNames() == null || dto.getMemberNames().size() < 2) {
            return Result.error("建群失败：除群主外，请至少勾选2名群成员！");
        }

        // 3. 生成唯一群号 (直接作为 chat_group 表的 id)
        String groupId = "GROUP_" + System.currentTimeMillis();

        // 4. 插入群基础信息 (适配你的 id 和 path 字段)
        ChatGroup group = new ChatGroup();
        group.setId(groupId);
        group.setGroupName(dto.getGroupName());
        group.setPath(dto.getGroupAvatar()); // 对应你数据库的 path
        group.setOwnerName(dto.getOwnerName());
        group.setCreateTime(new Date());
        chatGroupMapper.insert(group);

        // 5. 将群主加入成员表
        GroupMember owner = new GroupMember();
        owner.setGroupId(groupId);
        owner.setUserName(dto.getOwnerName());
        owner.setRole(2); // 群主
        owner.setJoinTime(new Date());
        groupMemberMapper.insert(owner);

        // 6. 遍历并加入勾选的好友
        for (String memberName : dto.getMemberNames()) {
            GroupMember member = new GroupMember();
            member.setGroupId(groupId);
            member.setUserName(memberName);
            member.setRole(0); // 普通成员
            member.setJoinTime(new Date());
            groupMemberMapper.insert(member);
        }

        return Result.success("群聊创建成功！", groupId);
    }

    @Override
    public Result getMyGroups(String userName) {
        QueryWrapper<GroupMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_name", userName);
        List<GroupMember> myMemberships = groupMemberMapper.selectList(memberQuery);

        if (myMemberships.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<String> groupIds = new ArrayList<>();
        for (GroupMember membership : myMemberships) {
            groupIds.add(membership.getGroupId());
        }

        // 🌟 注意：这里 in 的条件变成了 "id"，因为 chat_group 表主键叫 id
        QueryWrapper<ChatGroup> groupQuery = new QueryWrapper<>();
        groupQuery.in("id", groupIds);
        List<ChatGroup> myGroups = chatGroupMapper.selectList(groupQuery);

        return Result.success(myGroups);
    }
}