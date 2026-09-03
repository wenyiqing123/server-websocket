package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.GroupCreateDTO;
import cn.wyq.serverwebsocket.pojo.dto.GroupQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.ChatGroup;
import cn.wyq.serverwebsocket.service.GroupService;
import cn.wyq.serverwebsocket.socket.WebSocket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * GroupController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@RestController
@RequestMapping("/group")
@Tag(name = "群聊管理接口")
public class GroupController {

    @Autowired
    private GroupService groupService;


    @GetMapping("/page")
    @Operation(summary = "管理端：动态分页查询群聊列表")
    public PageResult<List<ChatGroup>> listGroups(GroupQueryDTO queryDTO) {

        // 赋予默认分页参数，防止前端漏传
        if (queryDTO.getPage() == null) queryDTO.setPage(1);
        if (queryDTO.getPageSize() == null) queryDTO.setPageSize(10);

        // 调用 Service 获取分页结果
        PageResult<List<ChatGroup>> pageResult = groupService.getGroupPage(queryDTO);

        // 使用标准的 Result 统一响应包装器返回
        return pageResult;
    }
//    @DeleteMapping("/admin-dismiss")
//    @Operation(summary = "强制解散群聊")
//    public Result<Void> dismissGroup(
//            @RequestParam Long groupId,
//            @RequestParam String operatorName) {
//
//        // 调用 Service 层执行解散逻辑
//        groupService.adminDismissGroup(groupId, operatorName);
//
//        return Result.success("群聊解散成功");
//    }


    @PostMapping("/create")
    @Operation(summary = "创建群聊并邀请成员")
    public Result createGroup(@RequestBody GroupCreateDTO dto) {
        if (dto.getGroupName() == null || dto.getOwnerName() == null) {
            return Result.error("群名称和群主不能为空");
        }
        Result result = groupService.createGroup(dto);

        // 直接广播，不进行响应码判断
        // 遍历所有被邀请的成员，挨个发系统广播（跳过群主自己）
        if (dto.getMemberNames() != null) {
            for (String memberName : dto.getMemberNames()) {
                if (!memberName.equals(dto.getOwnerName())) {
                    WebSocket.sendSystemBroadcast(memberName, "你被邀请加入了群聊 [" + dto.getGroupName() + "]，快去打个招呼吧！");
                }
            }
        }

        return result;
    }

    @GetMapping("/my")
    @Operation(summary = "获取我加入的群聊列表")
    public Result getMyGroups(@RequestParam("userName") String userName) {
        return groupService.getMyGroups(userName);
    }

    @GetMapping("/members")
    @Operation(summary = "获取群成员列表")
    public Result getGroupMembers(@RequestParam String groupId) {
        return Result.success(groupService.getGroupMembers(groupId));
    }

    /**
     * 1. 获取群成员详细列表（带头像、角色）
     */
    @GetMapping("/members/detail")
    public Result getGroupMembersDetail(@RequestParam("groupId") String groupId) {
        return groupService.getGroupMembersDetail(groupId);
    }

    /**
     * 2. 修改群信息
     * 建议前端传 JSON: { "groupId": "...", "groupName": "...", "groupAvatar": "...", "operatorName": "..." }
     */
    @PutMapping("/update")
    public Result updateGroupInfo(@RequestBody Map<String, String> params) {
        String groupId = params.get("groupId");
        String groupName = params.get("groupName");
        String groupAvatar = params.get("groupAvatar");
        String operatorName = params.get("operatorName");

        Result result = groupService.updateGroupInfo(groupId, groupName, groupAvatar, operatorName);

        // 直接广播，不进行响应码判断
        // 获取该群所有成员，通知他们群信息被修改了
        List<String> members = groupService.getGroupMemberNames("GROUP_" + groupId);
        if (members != null) {
            for (String member : members) {
                if (!member.equals(operatorName)) {
                    WebSocket.sendSystemBroadcast(member, "你所在的群聊 [" + groupName + "] 的信息已被管理员更新。");
                }
            }
        }

        return result;
    }

    /**
     * 3. 踢出群成员
     */
    @DeleteMapping("/kick")
    public Result kickGroupMember(
            @RequestParam("groupId") String groupId,
            @RequestParam("targetUserName") String targetUserName,
            @RequestParam("operatorName") String operatorName) {

        Result result = groupService.kickGroupMember(groupId, targetUserName, operatorName);

        // 直接广播，不进行响应码判断
        // 通知被踢的人
        WebSocket.sendSystemBroadcast(targetUserName, "你已被管理员 [" + operatorName + "] 移出群聊。");

        return result;
    }

    /**
     * 4. 退出群聊
     */
    @DeleteMapping("/quit")
    public Result quitGroup(
            @RequestParam("groupId") String groupId,
            @RequestParam("userName") String userName) {

        Result result = groupService.quitGroup(groupId, userName);

        // 直接广播，不进行响应码判断
        // 通知退群者本人（或者也可选择不通知，由前端直接提示）
        WebSocket.sendSystemBroadcast(userName, "你已成功退出群聊。");

        return result;
    }

    /**
     * 5. 解散群聊
     */
    @DeleteMapping("/dismiss")
    public Result dismissGroup(
            @RequestParam("groupId") String groupId,
            @RequestParam("operatorName") String operatorName) {

        // 1. 获取名单（提前获取，防删后找不到）
        List<String> members = groupService.getGroupMemberNames("GROUP_" + groupId);

        // 2. 执行解散核心逻辑
        Result result = groupService.dismissGroup(groupId, operatorName);

        // 3. 🌟 核心修复：必须判断后端真的解散成功了 (状态码为 200)，才能发送广播！
        if (result.get("code") != null && result.get("code") .equals("200") ) {
            if (members != null) {
                for (String member : members) {
                    // 不给自己发通知
                    if (!member.equals(operatorName)) {
                        WebSocket.sendSystemBroadcast(member, "你所在的群聊已被 [" + operatorName + "] 解散。");
                    }
                }
            }
        }

        return result;
    }
}