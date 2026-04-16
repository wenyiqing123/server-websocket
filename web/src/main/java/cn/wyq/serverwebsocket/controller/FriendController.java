package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/friend")
@Tag(name = "好友管理接口")
public class FriendController {

    @Autowired
    private FriendService friendService;

    // 在 cn.wyq.serverwebsocket.controller.FriendController 中添加：

    // ... 原有的 applyFriend 方法 ...

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
        return friendService.deleteFriend(userName, friendName);
    }

    @PutMapping("/accept/{id}")
    @Operation(summary = "同意好友申请")
    public Result acceptFriend(@PathVariable("id") Integer id, @RequestParam("userName") String userName) {
        if (id == null || userName == null || userName.trim().isEmpty()) {
            return Result.error("参数不完整");
        }
        return friendService.acceptFriend(id, userName);
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

        return friendService.applyFriend(userName, friendName);
    }
}