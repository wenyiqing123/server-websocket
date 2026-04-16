package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.GroupCreateDTO;
import cn.wyq.serverwebsocket.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/group")
@Tag(name = "群聊管理接口")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping("/create")
    @Operation(summary = "创建群聊并邀请成员")
    public Result createGroup(@RequestBody GroupCreateDTO dto) {
        if (dto.getGroupName() == null || dto.getOwnerName() == null) {
            return Result.error("群名称和群主不能为空");
        }
        return groupService.createGroup(dto);
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
        return groupService.updateGroupInfo(groupId, groupName, groupAvatar, operatorName);
    }

    /**
     * 3. 踢出群成员
     */
    @DeleteMapping("/kick")
    public Result kickGroupMember(
            @RequestParam("groupId") String groupId,
            @RequestParam("targetUserName") String targetUserName,
            @RequestParam("operatorName") String operatorName) {
        return groupService.kickGroupMember(groupId, targetUserName, operatorName);
    }

    /**
     * 4. 退出群聊
     */
    @DeleteMapping("/quit")
    public Result quitGroup(
            @RequestParam("groupId") String groupId,
            @RequestParam("userName") String userName) {
        return groupService.quitGroup(groupId, userName);
    }

    /**
     * 5. 解散群聊
     */
    @DeleteMapping("/dismiss")
    public Result dismissGroup(
            @RequestParam("groupId") String groupId,
            @RequestParam("operatorName") String operatorName) {
        return groupService.dismissGroup(groupId, operatorName);
    }
}