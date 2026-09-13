package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.AiAgentCreateDTO;
import cn.wyq.serverwebsocket.pojo.dto.AiBuiltinAgentUpdateDTO;
import cn.wyq.serverwebsocket.pojo.dto.AiAgentUpdateDTO;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.AiAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 自定义智能体接口，区分普通用户自有资源和管理员管理接口。
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI智能体", description = "用户自定义智能体及管理员管理接口")
public class AiAgentController {
    private final AiAgentService aiAgentService;

    @GetMapping("/builtin/girlfriend")
    @Operation(summary = "获取内置女友智能体配置")
    public Result getBuiltinGirlfriend(@CurrentUser UserEntity currentUser) {
        requireCurrentUser(currentUser);
        return Result.success(aiAgentService.getBuiltinGirlfriend());
    }

    @GetMapping("/agents")
    @Operation(summary = "获取我的智能体")
    public Result listMine(@CurrentUser UserEntity currentUser) {
        return Result.success(aiAgentService.listMine(requireCurrentUser(currentUser).getId()));
    }

    @PostMapping("/agents")
    @Operation(summary = "创建智能体")
    public Result create(@RequestBody AiAgentCreateDTO dto, @CurrentUser UserEntity currentUser) {
        return Result.success(aiAgentService.create(requireCurrentUser(currentUser).getId(), dto));
    }

    @GetMapping("/agents/{id}")
    @Operation(summary = "获取我的智能体详情")
    public Result getMine(@PathVariable Long id, @CurrentUser UserEntity currentUser) {
        return Result.success(aiAgentService.getMine(id, requireCurrentUser(currentUser).getId()));
    }

    @PutMapping("/agents/{id}")
    @Operation(summary = "修改我的智能体")
    public Result updateMine(@PathVariable Long id, @RequestBody AiAgentUpdateDTO dto,
                             @CurrentUser UserEntity currentUser) {
        return Result.success(aiAgentService.updateOwned(id, requireCurrentUser(currentUser).getId(), dto));
    }

    @DeleteMapping("/agents/{id}")
    @Operation(summary = "删除我的智能体")
    public Result deleteMine(@PathVariable Long id, @CurrentUser UserEntity currentUser) {
        aiAgentService.deleteOwned(id, requireCurrentUser(currentUser).getId());
        return Result.success("删除成功");
    }

    @GetMapping("/admin/agents")
    @Operation(summary = "管理员查询智能体")
    public Result listForAdmin(@RequestParam(required = false) String ownerUserName,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) Integer status,
                               @CurrentUser UserEntity currentUser) {
        return Result.success(aiAgentService.listForAdmin(currentUser, ownerUserName, name, status));
    }

    @GetMapping("/admin/agents/{id}")
    @Operation(summary = "管理员查看智能体")
    public Result getForAdmin(@PathVariable Long id, @CurrentUser UserEntity currentUser) {
        return Result.success(aiAgentService.getForAdmin(currentUser, id));
    }

    @PutMapping("/admin/agents/{id}")
    @Operation(summary = "管理员编辑智能体")
    public Result updateForAdmin(@PathVariable Long id, @RequestBody AiAgentUpdateDTO dto,
                                 @CurrentUser UserEntity currentUser) {
        return Result.success(aiAgentService.updateForAdmin(currentUser, id, dto));
    }

    @PatchMapping("/admin/agents/{id}/status")
    @Operation(summary = "管理员启用或禁用智能体")
    public Result updateStatus(@PathVariable Long id, @RequestParam Integer status,
                               @CurrentUser UserEntity currentUser) {
        aiAgentService.updateStatus(currentUser, id, status);
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/admin/agents/{id}")
    @Operation(summary = "管理员软删除智能体")
    public Result deleteForAdmin(@PathVariable Long id, @CurrentUser UserEntity currentUser) {
        aiAgentService.deleteForAdmin(currentUser, id);
        return Result.success("删除成功");
    }

    @GetMapping("/admin/builtin/girlfriend")
    @Operation(summary = "管理员查看内置女友智能体")
    public Result getBuiltinForAdmin(@CurrentUser UserEntity currentUser) {
        return Result.success(aiAgentService.getBuiltinGirlfriendForAdmin(currentUser));
    }

    @PutMapping("/admin/builtin/girlfriend")
    @Operation(summary = "管理员编辑内置女友智能体")
    public Result updateBuiltin(@RequestBody AiBuiltinAgentUpdateDTO dto,
                                @CurrentUser UserEntity currentUser) {
        return Result.success(aiAgentService.updateBuiltinGirlfriend(currentUser, dto));
    }

    @PostMapping("/admin/builtin/girlfriend/reset")
    @Operation(summary = "恢复内置女友默认配置")
    public Result resetBuiltin(@CurrentUser UserEntity currentUser) {
        return Result.success(aiAgentService.resetBuiltinGirlfriend(currentUser));
    }

    @PatchMapping("/admin/builtin/girlfriend/status")
    @Operation(summary = "启用或禁用内置女友智能体")
    public Result updateBuiltinStatus(@RequestParam Integer status,
                                      @CurrentUser UserEntity currentUser) {
        aiAgentService.updateBuiltinGirlfriendStatus(currentUser, status);
        return Result.success("状态更新成功");
    }

    private UserEntity requireCurrentUser(UserEntity currentUser) {
        if (currentUser == null || currentUser.getId() <= 0) {
            throw new cn.wyq.serverwebsocket.exception.ServiceException("请先登录", 401);
        }
        return currentUser;
    }
}
