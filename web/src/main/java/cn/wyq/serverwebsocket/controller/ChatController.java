package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.pojo.dto.AiChatRequestDTO;
import cn.wyq.serverwebsocket.pojo.dto.AiReplyDto;
import cn.wyq.serverwebsocket.pojo.dto.DocumentSessionDocumentsDTO;
import cn.wyq.serverwebsocket.pojo.entity.Chat;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.*;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ChatController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@RestController
@RequestMapping("/ai")
@Slf4j
@Tag(name = "AI聊天", description = "AI聊天与多会话管理")
@RequiredArgsConstructor
public class ChatController {

    private static final java.util.Set<String> BUILTIN_AGENT_TYPES = java.util.Set.of(
            "girlfriend", "service", "document");

    private final ChatService chatService;
    private final AiAgentService aiAgentService;
    private final DocumentService documentService; // 💡 1. 注入 DocumentService
    private final DocumentSessionService documentSessionService;
    private final ServiceQuestionSuggestionService serviceQuestionSuggestionService;
    private final SysConfigService sysConfigService;
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI聊天接口", description = "流式聊天")
    public Flux<String> chat(
            @RequestBody AiChatRequestDTO request,
            @CurrentUser UserEntity currentUser
    ) {
        if (request == null || !org.springframework.util.StringUtils.hasText(request.getMessage())
                || !org.springframework.util.StringUtils.hasText(request.getSessionId())) {
            throw new ServiceException("消息和会话编号不能为空", 400);
        }
        int maxMessageLength = configuredMaxMessageLength();
        if (request.getMessage().length() > maxMessageLength) {
            throw new ServiceException("单条消息不能超过 " + maxMessageLength + " 个字符", 400);
        }
        String sessionId = request.getSessionId();
        String agentType = org.springframework.util.StringUtils.hasText(request.getAgentType())
                ? request.getAgentType() : "girlfriend";
        Long agentId = request.getAgentId();
        validateAgentMode(agentType, agentId);
        if (agentId == null && "girlfriend".equalsIgnoreCase(agentType)) {
            aiAgentService.requireUsableBuiltinGirlfriend();
        }
        ensureSessionOwner(sessionId, agentType, agentId, currentUser, true);
        java.util.List<String> requestedDocIds = new java.util.ArrayList<>();
        if (request.getDocIds() != null) {
            requestedDocIds.addAll(request.getDocIds());
        }
        if (org.springframework.util.StringUtils.hasText(request.getDocId())) {
            requestedDocIds.add(request.getDocId());
        }
        if ("document".equalsIgnoreCase(agentType)) {
            java.util.List<String> associatedDocIds = documentSessionService
                    .listDocumentIds(sessionId, currentUser.getId());
            if (requestedDocIds.isEmpty()) {
                requestedDocIds.addAll(associatedDocIds);
            } else if (!associatedDocIds.containsAll(requestedDocIds)) {
                throw new ServiceException("请求的文档未关联到当前会话", 403);
            }
        }
        String message = request.getMessage().trim();
        // 先落库再请求模型，保证模型超时、断开或返回异常时用户问题仍然被记录。
        if ("service".equalsIgnoreCase(agentType)) {
            serviceQuestionSuggestionService.recordQuestion(currentUser.getId(), message);
        }
        return chatService.chatStream(message, sessionId, currentUser.getUserName(), agentType,
                agentId, currentUser.getId(), requestedDocIds);
    }

    /**
     * 根据用户正在输入的文字返回客服题库中的相似问题。
     * 该接口只访问预热后的本地题库，不调用大模型。
     */
    @GetMapping("/service/question-suggestions")
    @Operation(summary = "获取客服问题提示", description = "根据输入内容匹配相似客服问题")
    public Result getServiceQuestionSuggestions(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "5") int limit,
            @CurrentUser UserEntity currentUser) {
        if (!org.springframework.util.StringUtils.hasText(query)) {
            return Result.success(List.of());
        }
        int boundedLimit = Math.max(1, Math.min(limit, 10));
        return Result.success(serviceQuestionSuggestionService.suggest(query, boundedLimit));
    }

    @GetMapping("/history/{sessionId}")
    @Operation(summary = "加载历史记录", description = "返回官方 Message 列表")
    public Result getHistory(@PathVariable String sessionId, @RequestParam(defaultValue = "girlfriend") String agentType,
                             @RequestParam(required = false) Long agentId,
                             @CurrentUser UserEntity currentUser) {
        validateAgentMode(agentType, agentId);
        // 历史记录不触发模型调用；智能体被禁用或删除后，用户仍可以查看并清理自己的旧会话。
        ensureSessionOwner(sessionId, agentType, agentId, currentUser, false);
        return Result.success(chatService.getHistory(sessionId, agentType, agentId));
    }

    @GetMapping("/session/list")
    @Operation(summary = "获取会话列表", description = "查询当前用户拥有的所有聊天窗口")
    public Result getSessionList(@RequestParam(defaultValue = "girlfriend") String agentType,
                                 @RequestParam(required = false) Long agentId,
                                 @CurrentUser UserEntity currentUser) {
        validateAgentMode(agentType, agentId);
        if (agentId != null) {
            // 即使智能体已被禁用或软删除，也要先确认会话列表请求来自其所有者。
            aiAgentService.requireOwnedForSession(agentId, currentUser.getId());
        }
        // 仅查询当前用户名下的会话。即使智能体已禁用或软删除，也应允许用户看到旧记录并删除。
        return Result.success(chatService.getSessionList(currentUser.getId(), agentType, agentId));
    }

    @PostMapping("/session/create")
    @Operation(summary = "创建新会话", description = "创建一个新的聊天窗口")
    public Result createSession(@RequestParam String title, @RequestParam(defaultValue = "girlfriend") String agentType,
                                @RequestParam(required = false) Long agentId,
                                @CurrentUser UserEntity currentUser) {
        validateAgentMode(agentType, agentId);
        Integer promptVersion = null;
        if (agentId != null) {
            promptVersion = aiAgentService.requireUsable(agentId, currentUser.getId()).getPromptVersion();
        } else if ("girlfriend".equalsIgnoreCase(agentType)) {
            aiAgentService.requireUsableBuiltinGirlfriend();
        }
        return Result.success(chatService.createSession(currentUser.getUserName(), currentUser.getId(), title,
                agentType, agentId, promptVersion));
    }

    @GetMapping("/document/session/{sessionId}/documents")
    @Operation(summary = "获取文档会话附件")
    public Result getDocumentSessionDocuments(@PathVariable String sessionId,
                                              @CurrentUser UserEntity currentUser) {
        documentSessionService.assertOwned(sessionId, currentUser.getId());
        return Result.success(documentSessionService.listDocumentIds(
                sessionId, currentUser.getId()));
    }

    @PutMapping("/document/session/{sessionId}/documents")
    @Operation(summary = "更新文档会话附件")
    public Result updateDocumentSessionDocuments(@PathVariable String sessionId,
                                                 @RequestBody DocumentSessionDocumentsDTO dto,
                                                 @CurrentUser UserEntity currentUser) {
        java.util.List<String> documentIds = dto == null ? java.util.List.of() : dto.getDocIds();
        return Result.success(documentSessionService.replaceDocuments(
                sessionId, currentUser.getId(), documentIds));
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除会话", description = "删除左侧窗口记录及底层所有聊天记忆")
    public Result deleteSession(@PathVariable String sessionId,
                                @RequestParam(required = false) String agentType,
                                @RequestParam(required = false) Long agentId,
                                @CurrentUser UserEntity currentUser) {
        String resolvedAgentType = resolveAgentType(sessionId, agentType);
        validateAgentMode(resolvedAgentType, agentId);
        // 删除只需要确认会话归属和模式匹配。智能体被管理员禁用或删除后，
        // 用户仍应能清理自己的历史会话。
        ensureSessionOwner(sessionId, resolvedAgentType, agentId, currentUser, false);
        documentSessionService.removeSession(sessionId);
        chatService.deleteSession(sessionId, resolvedAgentType, agentId);
        return Result.success("删除成功");
    }
    @PostMapping("/uploadDoc")
    @Operation(summary = "上传并解析文档", description = "将文档切片并存入向量库")
    public Result uploadDocument(@RequestParam("file") MultipartFile file,
                                 @CurrentUser UserEntity currentUser) {
        try {
            String docId = documentService.uploadAndParse(file, currentUser.getId());
            return Result.success("文档解析成功", docId);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败", e);
            return Result.error("文档解析失败：" + e.getMessage());
        }
    }

    @GetMapping("/docList")
    @Operation(summary = "获取已上传的文档列表")
    public Result getDocList(@CurrentUser UserEntity currentUser) {
        return Result.success(documentService.listByOwner(currentUser.getId()));
    }

    @DeleteMapping("/document/{docId}")
    @Operation(summary = "删除文档", description = "删除物理文件、数据库记录及相关的聊天记忆")
    public Result deleteDocument(@PathVariable String docId, @CurrentUser UserEntity currentUser) {
        try {
            // 服务层先校验归属；确认是当前用户的文档后，再清理旧库中可能残留的会话关联。
            documentService.deleteDocument(docId, currentUser.getId());
            documentSessionService.removeDocument(docId);

            chatService.deleteSession("file_session_" + docId);

            return Result.success("删除成功");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除文档失败", e);
            return Result.error("删除文档失败: " + e.getMessage());
        }
    }

    @GetMapping("/document/{docId}/download")
    @Operation(summary = "下载已上传文档", description = "仅允许文档上传者下载其私有 AI 文档")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String docId,
                                                     @CurrentUser UserEntity currentUser) {
        DocumentService.DownloadableDocument document = documentService
                .loadOwnedDocument(docId, currentUser.getId());
        MediaType contentType = MediaTypeFactory.getMediaType(document.fileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(document.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(document.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(document.resource());
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

    private void ensureSessionOwner(String sessionId, String agentType, Long agentId, UserEntity currentUser,
                                    boolean requireUsableAgent) {
        if (!org.springframework.util.StringUtils.hasText(sessionId)) {
            throw new ServiceException("会话编号不能为空", 400);
        }
        if (currentUser == null || currentUser.getId() <= 0
                || !org.springframework.util.StringUtils.hasText(currentUser.getUserName())) {
            throw new ServiceException("请先登录", 401);
        }
        if (sessionId.startsWith("file_session_")) {
            throw new ServiceException("旧版文档会话已停用，请在文档分析中新建会话", 410);
        }
        Chat session = chatService.getSession(sessionId);
        if (session == null) {
            throw new ServiceException("会话不存在", 404);
        }
        if (!java.util.Objects.equals(currentUser.getId(), session.getOwnerUserId())) {
            throw new ServiceException("无权访问该工单会话", 403);
        }
        if (!java.util.Objects.equals(agentId, session.getAgentId())) {
            throw new ServiceException("无权访问该工单会话", 403);
        }
        if (agentId != null) {
            if (!"custom".equalsIgnoreCase(agentType)) {
                throw new ServiceException("无权访问该工单会话", 403);
            }
            if (requireUsableAgent) {
                aiAgentService.requireUsable(agentId, currentUser.getId());
            } else {
                aiAgentService.requireOwnedForSession(agentId, currentUser.getId());
            }
            return;
        }

        // 旧内置会话没有单独的 agent_type 字段，编号前缀就是其唯一可信来源。
        String expectedPrefix = agentType.toLowerCase(java.util.Locale.ROOT) + "_session_";
        if (!sessionId.startsWith(expectedPrefix)) {
            throw new ServiceException("无权访问该工单会话", 403);
        }
    }

    private void validateAgentMode(String agentType, Long agentId) {
        if (!org.springframework.util.StringUtils.hasText(agentType)) {
            throw new ServiceException("会话模式不能为空", 400);
        }
        if (agentId != null && !"custom".equalsIgnoreCase(agentType)) {
            throw new ServiceException("自定义智能体只能使用 custom 会话模式", 400);
        }
        if (agentId == null && "custom".equalsIgnoreCase(agentType)) {
            throw new ServiceException("自定义智能体编号不能为空", 400);
        }
        if (agentId == null && !BUILTIN_AGENT_TYPES.contains(agentType.toLowerCase(java.util.Locale.ROOT))) {
            throw new ServiceException("不支持该会话模式", 400);
        }
    }

    private String resolveAgentType(String sessionId, String agentType) {
        if (org.springframework.util.StringUtils.hasText(agentType)) {
            return agentType;
        }
        if (sessionId.startsWith("document_")) {
            return "document";
        }
        if (sessionId.startsWith("girlfriend_")) {
            return "girlfriend";
        }
        return "service";
    }

    /** 读取可动态调整的消息长度上限，配置异常时保持原有默认值。 */
    private int configuredMaxMessageLength() {
        try {
            return SystemConfigUtils.intValue(sysConfigService.getConfigMap(),
                    "chat_max_message_length", 10000, 1, 1_000_000);
        } catch (RuntimeException e) {
            log.warn("读取消息长度配置失败，将使用默认值", e);
            return 10000;
        }
    }
}
