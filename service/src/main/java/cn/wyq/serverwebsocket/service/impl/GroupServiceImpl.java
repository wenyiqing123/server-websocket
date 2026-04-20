package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.mapper.ChatGroupMapper;
import cn.wyq.serverwebsocket.mapper.GroupMemberMapper;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.dto.GroupCreateDTO;
import cn.wyq.serverwebsocket.pojo.dto.GroupQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.ChatGroup;
import cn.wyq.serverwebsocket.pojo.entity.GroupMember;
import cn.wyq.serverwebsocket.pojo.entity.UserEmailApplyEntity;
import cn.wyq.serverwebsocket.pojo.vo.GroupMemberQueryVo;
import cn.wyq.serverwebsocket.service.GroupService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private ChatGroupMapper chatGroupMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result getGroupMembersDetail(String groupId) {
        // 1. 查出群内所有成员
        QueryWrapper<GroupMember> query = new QueryWrapper<>();
        query.eq("group_id", groupId);
        List<GroupMember> members = groupMemberMapper.selectList(query);

        // 2. 组装前端需要的详细信息（带头像）
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (GroupMember member : members) {
            Map<String, Object> item = new HashMap<>();
            item.put("userName", member.getUserName());
            item.put("role", member.getRole()); // 0-成员, 1-管理, 2-群主
            item.put("joinTime", member.getJoinTime());
            // 查用户头像
            item.put("path", userMapper.getStatusAndPathByUserName(member.getUserName()));
            resultList.add(item);
        }

        // 3. 按照角色排序（群主在前，管理员其次，普通成员在后）
        resultList.sort((a, b) -> Integer.compare((Integer) b.get("role"), (Integer) a.get("role")));

        return Result.success(resultList);
    }

    @Override
    public Result updateGroupInfo(String groupId, String groupName, String groupAvatar, String operatorName) {
        // 1. 校验操作者权限
        QueryWrapper<GroupMember> authQuery = new QueryWrapper<>();
        authQuery.eq("group_id", groupId).eq("user_name", operatorName);
        GroupMember operator = groupMemberMapper.selectOne(authQuery);

        if (operator == null || operator.getRole() < 1) {
            return Result.error("权限不足：仅群主或管理员可修改群信息");
        }

        // 2. 更新 chat_group 表
        ChatGroup group = new ChatGroup();
        group.setId(groupId);
        if (groupName != null && !groupName.trim().isEmpty()) group.setGroupName(groupName);
        if (groupAvatar != null && !groupAvatar.trim().isEmpty()) group.setPath(groupAvatar);

        chatGroupMapper.updateById(group);
        return Result.success("群信息修改成功");
    }

    @Override
    public Result kickGroupMember(String groupId, String targetUserName, String operatorName) {
        // 1. 不能踢自己
        if (targetUserName.equals(operatorName)) return Result.error("不能将自己踢出群聊");

        // 2. 获取操作者和被踢者的信息
        QueryWrapper<GroupMember> opQuery = new QueryWrapper<>();
        opQuery.eq("group_id", groupId).eq("user_name", operatorName);
        GroupMember operator = groupMemberMapper.selectOne(opQuery);

        QueryWrapper<GroupMember> targetQuery = new QueryWrapper<>();
        targetQuery.eq("group_id", groupId).eq("user_name", targetUserName);
        GroupMember target = groupMemberMapper.selectOne(targetQuery);

        if (operator == null || target == null) return Result.error("群成员信息异常");

        // 3. 严格的权限校验
        // 群主(2)可以踢任何人(1, 0)
        // 管理员(1)只能踢普通成员(0)
        if (operator.getRole() == 0 || operator.getRole() <= target.getRole()) {
            return Result.error("权限不足：无法踢出该成员");
        }

        // 4. 执行删除
        groupMemberMapper.delete(targetQuery);
        return Result.success("已将 " + targetUserName + " 移出群聊");
    }

    @Override
    public Result quitGroup(String groupId, String userName) {
        QueryWrapper<GroupMember> query = new QueryWrapper<>();
        query.eq("group_id", groupId).eq("user_name", userName);
        GroupMember member = groupMemberMapper.selectOne(query);

        if (member == null) return Result.error("您不在该群聊中");

        // 如果是群主退群，提示必须解散
        if (member.getRole() == 2) {
            return Result.error("群主无法直接退出，请选择【解散群聊】");
        }

        groupMemberMapper.delete(query);
        return Result.success("已成功退出群聊");
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及多表操作，必须加事务
    public Result dismissGroup(String groupId, String operatorName) {

        // --- 🌟 第一重校验：检查操作者是否为【系统管理员】 ---
        // 假设你的系统管理员 role 是 1 (根据你之前的代码推测)
        UserEmailApplyEntity currentUser = userMapper.findOneByUsername(operatorName);
        boolean isSystemAdmin = (currentUser != null && (currentUser.getRole() == 1||currentUser.getRole() == 0));

        // --- 🌟 第二重校验：如果不是系统管理员，检查他是不是该群的【群主】 ---
        if (!isSystemAdmin) {
            QueryWrapper<GroupMember> query = new QueryWrapper<>();
            query.eq("group_id", groupId).eq("user_name", operatorName);
            GroupMember operator = groupMemberMapper.selectOne(query);

            if (operator == null || operator.getRole() != 2) {
                return Result.error("权限不足：仅群主或系统管理员可以解散群聊");
            }
        }

        // --- 权限校验通过，开始执行删除 ---

        // 1. 检查群是否存在（增加代码健壮性）
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            return Result.error("群聊不存在或已被解散");
        }

        // 2. 删除群聊基础信息
        chatGroupMapper.deleteById(groupId);

        // 3. 清空群成员表中的记录
        QueryWrapper<GroupMember> delMembersQuery = new QueryWrapper<>();
        delMembersQuery.eq("group_id", groupId);
        groupMemberMapper.delete(delMembersQuery);

        // 4. (可选) 清空该群的聊天记录
        /*
        QueryWrapper<Message> delMsgsQuery = new QueryWrapper<>();
        delMsgsQuery.eq("to_name", "GROUP_" + groupId); // 注意：这里通常带有前缀
        messageMapper.delete(delMsgsQuery);
        */

        return Result.success("群聊已成功解散");
    }
    @Override
    public Result getGroupMembers(String groupId) {
        List<GroupMemberQueryVo> groupMemberQueryVoList=  groupMemberMapper.queryGroupMembers(groupId);
        return Result.success(groupMemberQueryVoList);
    }
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
        group.setCreateTime(LocalDateTime.now());
        chatGroupMapper.insert(group);

        // 5. 将群主加入成员表
        GroupMember owner = new GroupMember();
        owner.setGroupId(groupId);
        owner.setUserName(dto.getOwnerName());
        owner.setRole(2); // 群主
        owner.setJoinTime(LocalDateTime.now());
        groupMemberMapper.insert(owner);

        // 6. 遍历并加入勾选的好友
        for (String memberName : dto.getMemberNames()) {
            GroupMember member = new GroupMember();
            member.setGroupId(groupId);
            member.setUserName(memberName);
            member.setRole(0); // 普通成员
            member.setJoinTime(LocalDateTime.now());
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

    @Override
    public PageResult<List<ChatGroup>> getGroupPage(GroupQueryDTO queryDTO) {
        // 1. 开启 PageHelper 分页拦截
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());

        // 2. 执行数据库查询 (此时 MyBatis 执行的 SQL 会被自动注入 LIMIT)
        Page<ChatGroup> page = chatGroupMapper.selectGroupList(queryDTO);

        // 3. 封装返回标准的 PageResult
        long total = page.getTotal();
        List<ChatGroup> records = page.getResult();

        return PageResult.success(total, records);
        // 假设你的 PageResult 有 success(long total, List data) 这个静态方法
    }
}