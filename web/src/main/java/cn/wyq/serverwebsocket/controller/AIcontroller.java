package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.common.AjaxResult;
import cn.wyq.serverwebsocket.pojo.dto.ConversationMessageDTO;
import cn.wyq.serverwebsocket.pojo.dto.UpdateConversationNameDTO;
import cn.wyq.serverwebsocket.pojo.entity.Conversation;
import cn.wyq.serverwebsocket.pojo.entity.ConversationMessage;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.AIService;
import cn.wyq.serverwebsocket.service.RealTimeRecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI相关", description = "AI相关接口")
@Slf4j
@RequiredArgsConstructor
public class AIcontroller {
    private final AIService aiService;
    private final RealTimeRecommendService recommendService;

    /**
     * 前端轮询获取推荐问题
     */
    @GetMapping("/recommend")
    @Operation(summary = "获取推荐消息", description = "获取推荐消息")
    public AjaxResult<List<String>> getRecommends(@RequestParam String currentTag) {
        log.info("请求获取推荐消息，当前标签 currentTag: {}", currentTag);
        List<String> recommends = recommendService.getRecommendations(currentTag);
        return AjaxResult.success(recommends);
    }

    // 1. 获取左侧侧边栏：历史对话列表
    @Operation(summary = "获取历史对话列表", description = "获取历史对话列表，按更新时间倒序排列")
    @GetMapping("/history")
    public AjaxResult<List<Conversation>> getHistory(@RequestParam String userName) {
        log.info("请求获取AI历史对话列表，用户名 userName: {}", userName);
        return AjaxResult.success(aiService.list(userName));
    }

    // 2. 点击某个对话：获取该对话的所有消息详情
    @Operation(summary = "获取该对话的所有消息详情", description = "获取该对话的所有消息详情")
    @GetMapping("/messages/{conversationId}")
    public AjaxResult<List<ConversationMessage>> getMessages(@PathVariable Integer conversationId) {
        log.info("请求获取对话消息记录详情，对话ID conversationId: {}", conversationId);
        return AjaxResult.success(aiService.getMessages(conversationId));
    }

    // 3. 核心功能：发送消息
    @PostMapping("/send")
    @Operation(summary = "发送消息", description = "发送消息到AI模型，返回模型回复")
    public AjaxResult<Void> sendMessage(@RequestBody ConversationMessageDTO conversationMessageDTO) {
        log.info("请求发送AI消息，参数 payload: {}", conversationMessageDTO);
        aiService.saveConversationMessage(conversationMessageDTO);
        return AjaxResult.success();
    }

    // 4. 开启新对话
    @Operation(summary = "开启新对话", description = "开启新对话，返回新对话的ID")
    @PostMapping("/new")
    public AjaxResult<Integer> newConversation(@CurrentUser UserEntity currentUser) {
        log.info("请求开启新对话，当前操作用户: {}", currentUser.getUserName());
        return AjaxResult.success(aiService.createConversation(currentUser.getUserName()));
    }

    // 5. 更新对话名称
    @PostMapping("/update/name")
    @Operation(summary = "更新对话名称", description = "更新对话名称")
    public AjaxResult<Void> updateConversationName(@CurrentUser UserEntity user, @RequestBody UpdateConversationNameDTO updateConversationNameDTO) {
        log.info("请求更新对话名称，操作用户: {}, 参数 payload: {}", user.getUserName(), updateConversationNameDTO);
        aiService.updateConversationName(user, updateConversationNameDTO);
        return AjaxResult.success();
    }

    // 6. 删除对话
    @DeleteMapping("/conversation/delete/{id}")
    @Operation(summary = "删除对话", description = "删除对话")
    public AjaxResult<Void> deleteConversation(@CurrentUser UserEntity user, @PathVariable int id) {
        log.info("请求删除对话，操作用户: {}, 待删除对话ID: {}", user.getUserName(), id);
        aiService.deleteConversation(user, id);
        return AjaxResult.success();
    }
}