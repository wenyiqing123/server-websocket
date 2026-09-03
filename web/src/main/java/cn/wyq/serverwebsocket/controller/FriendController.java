package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.service.FriendService;
import cn.wyq.serverwebsocket.socket.WebSocket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FriendController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@RestController
@RequestMapping("/friend")
@Tag(name = "好友管理接口")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @GetMapping("/pending")
    @Operation(summary = "获取待处理的好友申请")
    public Result getPendingRequests(@RequestParam("userName") String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        return friendService.getPendingRequests(userName);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除好友")
    public Result deleteFriend(@RequestParam String userName, @RequestParam String friendName) {
        Result result = friendService.deleteFriend(userName, friendName);

        // 直接广播，不进行响应码判断
        WebSocket.sendSystemBroadcast(friendName, "用户 [" + userName + "] 已与你解除好友关系。");

        return result;
    }

    @PutMapping("/accept/{id}")
    @Operation(summary = "同意好友申请")
    public Result acceptFriend(@PathVariable("id") Integer id,
                               @RequestParam("userName") String userName,
                               @RequestParam(value = "applicantName", required = false) String applicantName) {

        if (id == null || userName == null || userName.trim().isEmpty()) {
            return Result.error("参数不完整");
        }

        Result result = friendService.acceptFriend(id, userName);

        // 直接广播，不进行响应码判断
            WebSocket.sendSystemBroadcast(applicantName, "用户 [" + userName + "] 已同意你的好友申请，现在可以开始聊天了！");
        return result;
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的真实好友列表")
    public Result getMyFriends(@RequestParam("userName") String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        return friendService.getMyFriends(userName);
    }

    @PostMapping("/apply")
    @Operation(summary = "发送好友申请")
    public Result applyFriend(@RequestBody Map<String, String> params) {
        String userName = params.get("userName");
        String friendName = params.get("friendName");

        if (userName == null || friendName == null || userName.trim().isEmpty() || friendName.trim().isEmpty()) {
            return Result.error("申请参数不能为空");
        }

        Result result = friendService.applyFriend(userName, friendName);

        // 直接广播，不进行响应码判断
        WebSocket.sendSystemBroadcast(friendName, "用户 [" + userName + "] 请求添加你为好友，请在好友通知中查看。");

        return result;
    }
}