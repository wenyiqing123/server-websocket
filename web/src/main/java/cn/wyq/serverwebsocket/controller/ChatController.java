package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.service.ChatService;
import cn.wyq.serverwebsocket.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@Slf4j
@Tag(name = "AI聊天", description = "AI聊天与多会话管理")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final DocumentService documentService; // 💡 1. 注入 DocumentService
    @GetMapping(value = "/chat", produces = "text/event-stream")
    @LoginNotRequired
    @Operation(summary = "AI聊天接口", description = "流式聊天")
    public Flux<String> chat(
            @RequestParam String message,
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "泽水") String userName,
            @RequestParam String agentType,
            // 💡 新增：接收可选的 docId，用于文档专属分析
            @RequestParam(required = false) String docId
    ) {
        // 将 docId 传递给 Service
        return chatService.chatStream(message, sessionId, userName, agentType, docId);
    }

    @GetMapping("/history/{sessionId}")
    @LoginNotRequired
    @Operation(summary = "加载历史记录", description = "返回官方 Message 列表")
    public Result getHistory(@PathVariable String sessionId, @RequestParam String agentType) {
        return Result.success(chatService.getHistory(sessionId, agentType));
    }

    @GetMapping("/session/list")
    @LoginNotRequired
    @Operation(summary = "获取会话列表", description = "查询当前用户拥有的所有聊天窗口")
    public Result getSessionList(@RequestParam String agentType) {
        return Result.success(chatService.getSessionList("admin", agentType));
    }

    @PostMapping("/session/create")
    @LoginNotRequired
    @Operation(summary = "创建新会话", description = "创建一个新的聊天窗口")
    public Result createSession(@RequestParam String title, @RequestParam String agentType) {
        return Result.success(chatService.createSession("admin", title, agentType));
    }

    @DeleteMapping("/session/{sessionId}")
    @LoginNotRequired
    @Operation(summary = "删除会话", description = "删除左侧窗口记录及底层所有聊天记忆")
    public Result deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return Result.success("删除成功");
    }
    @PostMapping("/uploadDoc")
    @LoginNotRequired
    @Operation(summary = "上传并解析文档", description = "将文档切片并存入向量库")
    public Result uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            // 调用你已经写好的 Service 逻辑
            String docId = documentService.uploadAndParse(file);
            return Result.success("文档解析成功", docId);
        } catch (Exception e) {
            log.error("文档解析失败", e);
            return Result.error("文档解析失败：" + e.getMessage());
        }
    }

    @GetMapping("/docList")
    @LoginNotRequired
    @Operation(summary = "获取已上传的文档列表")
    public Result getDocList() {
        // 直接调用 Service 的 list() 方法拉取 sys_document 所有的记录
        return Result.success(documentService.list());
    }

    // 🌟 新增：彻底删除文档及其聊天记忆
    @DeleteMapping("/document/{docId}")
    @LoginNotRequired
    @Operation(summary = "删除文档", description = "删除物理文件、数据库记录及相关的聊天记忆")
    public Result deleteDocument(@PathVariable String docId) {
        try {
            // 1. 删除文件和记录
            documentService.deleteDocument(docId);

            // 2. 清理相关的聊天记忆 (拼装上之前定好的前缀)
            chatService.deleteSession("file_session_" + docId);

            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除文档失败", e);
            return Result.error("删除文档失败: " + e.getMessage());
        }
    }
}