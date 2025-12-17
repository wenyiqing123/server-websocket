package cn.wqk.serverwebsocket.controller;

import cn.wqk.serverwebsocket.framework.common.AjaxResult;
import cn.wqk.serverwebsocket.pojo.dto.ChatRequestDto;
import cn.wqk.serverwebsocket.pojo.entity.Conversation;
import cn.wqk.serverwebsocket.pojo.entity.ConversationMessage;
import cn.wqk.serverwebsocket.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/ai")
@Tag(name = "AI相关")
public class AIcontroller {
    @Autowired
    private AIService aiService;

    // 1. 获取左侧侧边栏：历史对话列表
    @GetMapping("/history")
    public AjaxResult<List<Conversation>> getHistory() {
        // 逻辑：查询 conversation 表，按 update_time 倒序排列
        return AjaxResult.success(aiService.list());
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
    public AjaxResult<Void> sendMessage(@ParameterObject ConversationMessage conversationMessage) {
        // 逻辑较复杂，详见 Service 层说明
        aiService.saveConversationMessage(conversationMessage);
        return AjaxResult.success();
    }

    // 4. 开启新对话
    @PostMapping("/new")
    public AjaxResult<Integer> newConversation() {
        return AjaxResult.success(aiService.createConversation("新对话"));
    }

    // 3. 核心功能：前端发送用户消息
    @PostMapping("/send/user")
    public AjaxResult<String> sendUserMessage(@RequestBody ChatRequestDto request) {
        // 后端只负责存储用户消息
        aiService.saveUserMessage(request);
        return AjaxResult.success("用户消息存储成功");
    }


}
