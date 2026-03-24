package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@Slf4j
@Tag(name = "AI聊天", description = "AI聊天与多会话管理")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/chat")
    @LoginNotRequired
    @Operation(summary = "AI聊天接口", description = "流式聊天")
    public Flux<String> chat(
            @RequestParam String message,
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "宝宝") String userName
    ) {
        return chatService.chatStream(message, sessionId, userName);
    }

    @GetMapping("/history/{sessionId}")
    @LoginNotRequired
    @Operation(summary = "加载历史记录", description = "返回官方 Message 列表")
    public Result getHistory(@PathVariable String sessionId) {
        return Result.success(chatService.getHistory(sessionId));
    }

    @GetMapping("/session/list")
    @LoginNotRequired
    @Operation(summary = "获取会话列表", description = "查询当前用户拥有的所有聊天窗口")
    public Result getSessionList() {
        // TODO: 接入鉴权后替换为真实 userId
        return Result.success(chatService.getSessionList("admin"));
    }

    @PostMapping("/session/create")
    @LoginNotRequired
    @Operation(summary = "创建新会话", description = "创建一个新的聊天窗口")
    public Result createSession(@RequestParam String title) {
        // TODO: 接入鉴权后替换为真实 userId
        return Result.success(chatService.createSession("admin", title));
    }

    @DeleteMapping("/session/{sessionId}")
    @LoginNotRequired
    @Operation(summary = "删除会话", description = "删除左侧窗口记录及底层所有聊天记忆")
    public Result deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return Result.success("删除成功");
    }
}