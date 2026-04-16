package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.mapper.FriendMapper;
import cn.wyq.serverwebsocket.mapper.MessageMapper;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.entity.Friend;
import cn.wyq.serverwebsocket.pojo.vo.FriendPathVO;
import cn.wyq.serverwebsocket.service.FriendService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {

    @Autowired
    private FriendMapper friendMapper;
    @Autowired
    private MessageMapper messageMapper;

    // 在 cn.wyq.serverwebsocket.service.impl.FriendServiceImpl 中添加：

    // ... 原有的 applyFriend 方法 ...

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteFriend(String userName, String friendName) {
        // 1. 删除我方的好友关系 (A -> B)
        friendMapper.deleteFriendRecord(userName, friendName);

        // 2. 删除对方的好友关系 (B -> A)
        friendMapper.deleteFriendRecord(friendName, userName);

        // 3. （可选）连带清空两人的聊天记录。如果你不想删记录，把这行注释掉即可
        messageMapper.deleteChatHistory(userName, friendName);

        return Result.success("好友已删除");
    }
    @Override
    public Result getPendingRequests(String userName) {
        // 查询条件：friend_name 是我，且 status 是 0（待处理）
        QueryWrapper<Friend> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("friend_name", userName).eq("status", 0);

        // 按时间倒序，最新的申请在最上面
        queryWrapper.orderByDesc("create_time");

        List<Friend> pendingList = friendMapper.selectList(queryWrapper);
        ArrayList<FriendPathVO> friendPathVOArrayList = new ArrayList<>();
        for (Friend friend : pendingList) {
            FriendPathVO friendPathVO = new FriendPathVO();
            BeanUtils.copyProperties(friend,friendPathVO);
            friendPathVO.setPath(userMapper.findOneByUsername(friend.getUserName()).getPath());
            friendPathVOArrayList.add(friendPathVO);
        }
        return Result.success(friendPathVOArrayList);
    }
    @Autowired
    private UserMapper userMapper;

    @Override
    public Result getMyFriends(String userName) {
        // 1. 查出所有 status=1 (已同意) 的好友账号
        QueryWrapper<Friend> friendQuery = new QueryWrapper<>();
        friendQuery.eq("user_name", userName).eq("status", 1);
        List<Friend> friends = friendMapper.selectList(friendQuery);

        if (friends.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 2. 提取所有好友的账号名称
        List<String> friendNames = friends.stream()
                .map(Friend::getFriendName)
                .collect(Collectors.toList());

        // 3. 去 user 表查出这些好友的头像信息
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        // 我们用 selectMaps 方便前端直接接收带驼峰的字段名
        userQuery.in("user_name", friendNames)
                .select("user_name AS userName", "path");

        List<Map<String, Object>> resultList = userMapper.selectMaps(userQuery);

        return Result.success(resultList);
    }
    @Override
    public Result acceptFriend(Integer id, String userName) {
        // 1. 根据 ID 查出这条申请记录
        Friend friendRequest = friendMapper.selectById(id);

        if (friendRequest == null) {
            return Result.error("好友申请记录不存在");
        }

        // 2. 安全校验：确认这条申请确实是发给当前登录用户的
        if (!friendRequest.getFriendName().equals(userName)) {
            return Result.error("无权操作此申请");
        }

        if (friendRequest.getStatus() == 1) {
            return Result.error("已同意过该申请，无需重复操作");
        }

        // 3. 将这条申请的状态改为 1 (已是好友)
        friendRequest.setStatus(1);
        friendMapper.updateById(friendRequest);

        // 4. 🌟 关键步骤：双向奔赴！自动生成一条反向的好友记录
        // 因为查询好友列表时，通常是查 user_name = 自己的记录
        QueryWrapper<Friend> reverseQuery = new QueryWrapper<>();
        reverseQuery.eq("user_name", userName).eq("friend_name", friendRequest.getUserName());
        Friend reverseFriend = friendMapper.selectOne(reverseQuery);

        if (reverseFriend == null) {
            // 如果没有反向记录，则插入一条
            Friend newFriend = new Friend();
            newFriend.setUserName(userName); // 发起人变成自己
            newFriend.setFriendName(friendRequest.getUserName()); // 对方变成好友
            newFriend.setStatus(1); // 直接是已同意状态
            friendMapper.insert(newFriend);
        } else if (reverseFriend.getStatus() != 1) {
            // 如果有记录但状态不是1，就更新为1
            reverseFriend.setStatus(1);
            friendMapper.updateById(reverseFriend);
        }

        return Result.success("已同意好友申请");
    }

    @Override
    public Result applyFriend(String userName, String friendName) {
        // 1. 防止添加自己
        if (userName.equals(friendName)) {
            return Result.error("不能添加自己为好友");
        }

        // 2. 检查自己是否已经发过申请
        QueryWrapper<Friend> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName).eq("friend_name", friendName);
        Friend existFriend = friendMapper.selectOne(queryWrapper);

        if (existFriend != null) {
            if (existFriend.getStatus() == 0) {
                return Result.error("您已经发送过申请，请等待对方通过");
            } else if (existFriend.getStatus() == 1) {
                return Result.error("你们已经是好友了，无需重复添加");
            } else {
                // 如果之前被拒绝了，允许重新发起申请，状态重置为 0
                existFriend.setStatus(0);
                friendMapper.updateById(existFriend);
                return Result.success("重新发送好友申请成功");
            }
        }

        // 3. 检查对方是不是恰好也给你发了申请（双向奔赴）
        QueryWrapper<Friend> reverseQuery = new QueryWrapper<>();
        reverseQuery.eq("user_name", friendName).eq("friend_name", userName);
        Friend reverseFriend = friendMapper.selectOne(reverseQuery);

        if (reverseFriend != null && reverseFriend.getStatus() == 0) {
            // 对方已经发了申请，直接同意对方
            reverseFriend.setStatus(1);
            friendMapper.updateById(reverseFriend);

            // 并自动生成一条自己的通过记录
            Friend newFriend = new Friend();
            newFriend.setUserName(userName);
            newFriend.setFriendName(friendName);
            newFriend.setStatus(1);
            friendMapper.insert(newFriend);
            return Result.success("对方也申请了您，已自动成为好友！");
        }

        // 4. 正常的新增申请记录
        Friend friend = new Friend();
        friend.setUserName(userName);
        friend.setFriendName(friendName);
        friend.setStatus(0); // 0表示待通过
        friendMapper.insert(friend);

        return Result.success("好友申请发送成功");
    }
}