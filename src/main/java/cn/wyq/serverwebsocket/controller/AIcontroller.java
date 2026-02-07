package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.framework.annotation.CurrentUser;
import cn.wyq.serverwebsocket.framework.common.AjaxResult;
import cn.wyq.serverwebsocket.pojo.dto.ConversationMessageDTO;
import cn.wyq.serverwebsocket.pojo.dto.UpdateConversationNameDTO;
import cn.wyq.serverwebsocket.pojo.entity.Conversation;
import cn.wyq.serverwebsocket.pojo.entity.ConversationMessage;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController

@RequestMapping("/ai")
@Tag(name = "AI相关", description = "AI相关接口")
public class AIcontroller {
    @Autowired
    private AIService aiService;

    // 1. 获取左侧侧边栏：历史对话列表
    @Operation(summary = "获取历史对话列表",
            description = "获取历史对话列表，按更新时间倒序排列")
    @GetMapping("/history")
    public AjaxResult<List<Conversation>> getHistory(@RequestParam String userName) {
        // 逻辑：查询 conversation 表，按 update_time 倒序排列
        return AjaxResult.success(aiService.list(userName));
    }

    // 2. 点击某个对话：获取该对话的所有消息详情
    @Operation(summary = "获取该对话的所有消息详情",
            description = "获取该对话的所有消息详情")
    @GetMapping("/messages/{conversationId}")
    public AjaxResult<List<ConversationMessage>> getMessages(@PathVariable Integer conversationId) {
        // 逻辑：查询 conversation_message 表，where conversation_id = ?
        return AjaxResult.success(aiService.getMessages(conversationId));
    }

    // 3. 核心功能：发送消息
    @PostMapping("/send")
    @Operation(summary = "发送消息",
            description = "发送消息到AI模型，返回模型回复")
    public AjaxResult<Void> sendMessage(@RequestBody ConversationMessageDTO conversationMessageDTO) {
        // 逻辑较复杂，详见 Service 层说明
        aiService.saveConversationMessage(conversationMessageDTO);
        return AjaxResult.success();
    }

    // 4. 开启新对话
    @Operation(summary = "开启新对话",
            description = "开启新对话，返回新对话的ID")
    @PostMapping("/new")
    public AjaxResult<Integer> newConversation(@CurrentUser UserEntity currentUser) {
        return AjaxResult.success(aiService.createConversation(currentUser.getUserName()));
    }

    @PostMapping("/update/name")
    @Operation(summary = "更新对话名称",
            description = "更新对话名称")
    public AjaxResult<Void> updateConversationName(@RequestBody UpdateConversationNameDTO updateConversationNameDTO) {
        aiService.updateConversationName(updateConversationNameDTO);
        return AjaxResult.success();
    }


}
