package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.annotation.LoginNotRequired;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.pojo.dto.AiReplyDto;
import cn.wyq.serverwebsocket.pojo.dto.DocumentSessionDocumentsDTO;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.ChatService;
import cn.wyq.serverwebsocket.service.DocumentService;
import cn.wyq.serverwebsocket.service.DocumentSessionService;
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
    private final DocumentSessionService documentSessionService;
    @GetMapping(value = "/chat", produces = "text/event-stream")
    @Operation(summary = "AI聊天接口", description = "流式聊天")
    public Flux<String> chat(
            @RequestParam String message,
            @RequestParam String sessionId,
            @RequestParam String agentType,
            @RequestParam(required = false) String docId,
            @RequestParam(required = false) java.util.List<String> docIds,
            @CurrentUser UserEntity currentUser
    ) {
        ensureSessionOwner(sessionId, agentType, currentUser.getUserName());
        java.util.List<String> requestedDocIds = new java.util.ArrayList<>();
        if (docIds != null) {
            requestedDocIds.addAll(docIds);
        }
        if (org.springframework.util.StringUtils.hasText(docId)) {
            requestedDocIds.add(docId);
        }
        if ("document".equalsIgnoreCase(agentType)) {
            java.util.List<String> associatedDocIds = documentSessionService
                    .listDocumentIds(sessionId, currentUser.getUserName());
            if (requestedDocIds.isEmpty()) {
                requestedDocIds.addAll(associatedDocIds);
            } else if (!associatedDocIds.containsAll(requestedDocIds)) {
                throw new ServiceException("请求的文档未关联到当前会话", 403);
            }
        }
        return chatService.chatStream(message, sessionId, currentUser.getUserName(), agentType, requestedDocIds);
    }

    @GetMapping("/history/{sessionId}")
    @Operation(summary = "加载历史记录", description = "返回官方 Message 列表")
    public Result getHistory(@PathVariable String sessionId, @RequestParam String agentType,
                             @CurrentUser UserEntity currentUser) {
        ensureSessionOwner(sessionId, agentType, currentUser.getUserName());
        return Result.success(chatService.getHistory(sessionId, agentType));
    }

    @GetMapping("/session/list")
    @Operation(summary = "获取会话列表", description = "查询当前用户拥有的所有聊天窗口")
    public Result getSessionList(@RequestParam String agentType, @CurrentUser UserEntity currentUser) {
        return Result.success(chatService.getSessionList(currentUser.getUserName(), agentType));
    }

    @PostMapping("/session/create")
    @Operation(summary = "创建新会话", description = "创建一个新的聊天窗口")
    public Result createSession(@RequestParam String title, @RequestParam String agentType,
                                @CurrentUser UserEntity currentUser) {
        return Result.success(chatService.createSession(currentUser.getUserName(), title, agentType));
    }

    @GetMapping("/document/session/{sessionId}/documents")
    @Operation(summary = "获取文档会话附件")
    public Result getDocumentSessionDocuments(@PathVariable String sessionId,
                                              @CurrentUser UserEntity currentUser) {
        documentSessionService.assertOwned(sessionId, currentUser.getUserName());
        return Result.success(documentSessionService.listDocumentIds(sessionId, currentUser.getUserName()));
    }

    @PutMapping("/document/session/{sessionId}/documents")
    @Operation(summary = "更新文档会话附件")
    public Result updateDocumentSessionDocuments(@PathVariable String sessionId,
                                                 @RequestBody DocumentSessionDocumentsDTO dto,
                                                 @CurrentUser UserEntity currentUser) {
        java.util.List<String> documentIds = dto == null ? java.util.List.of() : dto.getDocIds();
        return Result.success(documentSessionService.replaceDocuments(
                sessionId, currentUser.getUserName(), documentIds));
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除会话", description = "删除左侧窗口记录及底层所有聊天记忆")
    public Result deleteSession(@PathVariable String sessionId, @CurrentUser UserEntity currentUser) {
        String agentType = sessionId.startsWith("document_") ? "document" : "service";
        ensureSessionOwner(sessionId, agentType, currentUser.getUserName());
        documentSessionService.removeSession(sessionId);
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
            documentSessionService.removeDocument(docId);
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
    @PostMapping("/suggest-reply")
    @Operation(summary = "AI辅助聊天", description = "根据上下文生成高情商回复")
    public Result suggestReply(@RequestBody AiReplyDto dto, @CurrentUser UserEntity currentUser) {
        String reply = chatService.generateSuggestReply(dto, currentUser.getUserName());
        if (reply != null) {
            return Result.success(reply);
        }
        return Result.error("AI 暂时大脑短路了，请重试");
    }

    private void ensureSessionOwner(String sessionId, String agentType, String userName) {
        if (sessionId.startsWith("file_session_")) {
            return;
        }
        boolean owned = chatService.getSessionList(userName, agentType).stream()
                .anyMatch(session -> session.getId().equals(sessionId));
        if (!owned) {
            throw new ServiceException("无权访问该工单会话", 403);
        }
    }
}
