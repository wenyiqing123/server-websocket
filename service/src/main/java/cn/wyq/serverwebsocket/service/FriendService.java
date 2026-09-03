package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.common.Result;

import java.util.List;

/**
 * FriendService：业务服务接口，定义对应领域的业务操作契约。
 */
public interface FriendService {
    /**
     * 发起好友申请
     */
    Result applyFriend(String userName, String friendName);
    Result deleteFriend(String userName, String friendName);
    // 在 cn.wyq.serverwebsocket.service.FriendService 中添加：

    /**
     * 获取当前用户的待处理好友申请列表 (谁想加我)
     */


    Result getPendingRequests(String userName);

    /**
     * 获取我的真实好友列表（附带头像信息）
     */
    Result getMyFriends(String userName);
    /**
     * 同意好友申请
     */
    Result acceptFriend(Integer id, String userName);

    List<String> getMyFriendsNameByUsername(String userName);
}