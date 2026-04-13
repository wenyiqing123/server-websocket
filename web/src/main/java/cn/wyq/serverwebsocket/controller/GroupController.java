package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.GroupCreateDTO;
import cn.wyq.serverwebsocket.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}